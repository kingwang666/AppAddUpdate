#include <sys/types.h>
#include "bzip2/bzlib.h"
#include <err.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "util/android_log_print.h"
#include "com_wang_appupdate_util_PatchUtil.h"





static off_t offtin(u_char *buf)
{
    off_t y;

    y=buf[7]&0x7F;
    y=y*256;y+=buf[6];
    y=y*256;y+=buf[5];
    y=y*256;y+=buf[4];
    y=y*256;y+=buf[3];
    y=y*256;y+=buf[2];
    y=y*256;y+=buf[1];
    y=y*256;y+=buf[0];

    if(buf[7]&0x80) y=-y;

    return y;
}

int applypatch(int argc,char * argv[])
{
    FILE * f, * cpf, * dpf, * epf;
    BZFILE * cpfbz2, * dpfbz2, * epfbz2;
    int cbz2err, dbz2err, ebz2err;
    int fd;
    ssize_t oldsize,newsize;
    ssize_t bzctrllen,bzdatalen;
    u_char header[32],buf[8];
    u_char *old, *new;
    off_t oldpos,newpos;
    off_t ctrl[3];
    off_t lenread;
    off_t i;

    if(argc!=4) {
        LOGE("%s please usage: oldfile newfile patchfile three files", argv[0]);
        return 1;
    }

    /* Open patch file */
    if ((f = fopen(argv[3], "r")) == NULL) {
        LOGE("open %s fail", argv[3]);
        return 4;
    }

    /*
    File format:
        0	8	"BSDIFF40"
        8	8	X
        16	8	Y
        24	8	sizeof(newfile)
        32	X	bzip2(control block)
        32+X	Y	bzip2(diff block)
        32+X+Y	???	bzip2(extra block)
    with control block a set of triples (x,y,z) meaning "add x bytes
    from oldfile to x bytes from the diff block; copy y bytes from the
    extra block; seek forwards in oldfile by z bytes".
    */

    /* Read header */
    if (fread(header, 1, 32, f) < 32) {
        if (feof(f)){
            LOGE("Corrupt patch");
            return 9;
        }
        LOGE("read %s fail", argv[3]);
        return 4;
    }

    /* Check for appropriate magic */
    if (memcmp(header, "BSDIFF40", 8) != 0) {
        LOGE("Corrupt patch");
        return 9;
    }

    /* Read lengths from header */
    bzctrllen=offtin(header+8);
    bzdatalen=offtin(header+16);
    newsize=offtin(header+24);
    if((bzctrllen<0) || (bzdatalen<0) || (newsize<0)) {
        LOGE("Corrupt patch");
        return 9;
    }

    /* Close patch file and re-open it via libbzip2 at the right places */
    if (fclose(f)){
        LOGE("close %s fail", argv[3]);
        return 4;
    }
    if ((cpf = fopen(argv[3], "r")) == NULL) {
        LOGE("open %s fail", argv[3]);
        return 4;
    }
    if (fseeko(cpf, 32, SEEK_SET)){
        LOGE("seeko(%s, %lld) fail", argv[3], (long long)32);
        return 4;
    }
    if ((cpfbz2 = BZ2_bzReadOpen(&cbz2err, cpf, 0, 0, NULL, 0)) == NULL) {
        LOGE("BZ2_bzReadOpen, bz2err = %d", cbz2err);
        return 10;
    }
    if ((dpf = fopen(argv[3], "r")) == NULL) {
        LOGE("open %s fail", argv[3]);
        return 4;
    }
    if (fseeko(dpf, 32 + bzctrllen, SEEK_SET)) {
        LOGE("seeko(%s, %lld) fail", argv[3], (long long)32);
        return 4;
    }
    if ((dpfbz2 = BZ2_bzReadOpen(&dbz2err, dpf, 0, 0, NULL, 0)) == NULL) {
        LOGE("BZ2_bzReadOpen, bz2err = %d", cbz2err);
        return 10;
    }
    if ((epf = fopen(argv[3], "r")) == NULL) {
        LOGE("open %s fail", argv[3]);
        return 4;
    }
    if (fseeko(epf, 32 + bzctrllen + bzdatalen, SEEK_SET)) {
        LOGE("seeko(%s, %lld) fail", argv[3], (long long)32);
        return 4;
    }
    if ((epfbz2 = BZ2_bzReadOpen(&ebz2err, epf, 0, 0, NULL, 0)) == NULL) {
        LOGE("BZ2_bzReadOpen, bz2err = %d", cbz2err);
        return 10;
    }

    if(((fd=open(argv[1],O_RDONLY,0))<0) ||
       ((oldsize=lseek(fd,0,SEEK_END))==-1) ||
       ((old=malloc(oldsize+1))==NULL) ||
       (lseek(fd,0,SEEK_SET)!=0) ||
       (read(fd,old,oldsize)!=oldsize) ||
       (close(fd)==-1)) {
        LOGE("read %s is fail", argv[1]);
        return 2;
    }
    if((new=malloc(newsize+1))==NULL) {
        LOGE("Memory allocation is fail");
        return 5;
    }

    oldpos=0;newpos=0;
    while(newpos<newsize) {
        /* Read control data */
        for(i=0;i<=2;i++) {
            lenread = BZ2_bzRead(&cbz2err, cpfbz2, buf, 8);
            if ((lenread < 8) || ((cbz2err != BZ_OK) &&
                                  (cbz2err != BZ_STREAM_END))) {
                LOGE("Corrupt patch");
                return 9;
            }
            ctrl[i]=offtin(buf);
        };

        /* Sanity-check */
        if(newpos+ctrl[0]>newsize) {
            LOGE("Corrupt patch");
            return 9;
        }

        /* Read diff string */
        lenread = BZ2_bzRead(&dbz2err, dpfbz2, new + newpos, ctrl[0]);
        if ((lenread < ctrl[0]) ||
            ((dbz2err != BZ_OK) && (dbz2err != BZ_STREAM_END))) {
            LOGE("Corrupt patch");
            return 9;
        }

        /* Add old data to diff string */
        for(i=0;i<ctrl[0];i++)
            if((oldpos+i>=0) && (oldpos+i<oldsize))
                new[newpos+i]+=old[oldpos+i];

        /* Adjust pointers */
        newpos+=ctrl[0];
        oldpos+=ctrl[0];

        /* Sanity-check */
        if(newpos+ctrl[1]>newsize) {
            LOGE("Corrupt patch");
            return 9;
        }

        /* Read extra string */
        lenread = BZ2_bzRead(&ebz2err, epfbz2, new + newpos, ctrl[1]);
        if ((lenread < ctrl[1]) ||
            ((ebz2err != BZ_OK) && (ebz2err != BZ_STREAM_END))) {
            LOGE("Corrupt patch");
            return 9;
        }

        /* Adjust pointers */
        newpos+=ctrl[1];
        oldpos+=ctrl[2];
    };

    /* Clean up the bzip2 reads */
    BZ2_bzReadClose(&cbz2err, cpfbz2);
    BZ2_bzReadClose(&dbz2err, dpfbz2);
    BZ2_bzReadClose(&ebz2err, epfbz2);
    if (fclose(cpf) || fclose(dpf) || fclose(epf)) {
        LOGE("close %s fail", argv[3]);
        return 4;
    }

    /* Write the new file */
    if(((fd=open(argv[2],O_CREAT|O_TRUNC|O_WRONLY,0666))<0) ||
       (write(fd,new,newsize)!=newsize) || (close(fd)==-1)) {
        LOGE("write %s fail", argv[2]);
        return 10;
    }

    free(new);
    free(old);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_wang_appupdate_util_PatchUtil_patch
        (JNIEnv *env, jclass cls,
         jstring old, jstring new, jstring patch){
    int argc = 4;
    char * argv[argc];
    argv[0] = "bspatch";
    argv[1] = (char*) ((*env)->GetStringUTFChars(env, old, 0));
    argv[2] = (char*) ((*env)->GetStringUTFChars(env, new, 0));
    argv[3] = (char*) ((*env)->GetStringUTFChars(env, patch, 0));

    LOGD("old apk = %s \n", argv[1]);
    LOGD("patch = %s \n", argv[3]);
    LOGD("new apk = %s \n", argv[2]);

    int ret = applypatch(argc, argv);
    LOGD("patch result = %d ", ret);

    (*env)->ReleaseStringUTFChars(env, old, argv[1]);
    (*env)->ReleaseStringUTFChars(env, new, argv[2]);
    (*env)->ReleaseStringUTFChars(env, patch, argv[3]);
    return ret;
}