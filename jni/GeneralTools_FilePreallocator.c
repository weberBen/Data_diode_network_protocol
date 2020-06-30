
#define _GNU_SOURCE 

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>

#include "GeneralTools_FilePreallocator.h"

#define ERROR_MAX_SIZE 500
#define BUFFER_SIZE 1000

JNIEXPORT void JNICALL Java_GeneralTools_FilePreallocator__1preallocate (JNIEnv * env, jobject obj, jstring filename_obj, jlong length_obj)
{
    loff_t	length = (loff_t)length_obj;
	loff_t	offset = 0;

    if(length<0)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, exp_cls, "size of the file cannot be negative");

        return ;
    }

    const char * filename = (*env)->GetStringUTFChars(env, filename_obj, NULL);
    if(filename==NULL)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, exp_cls, "filename is NULL");

        //free
        (*env)->ReleaseStringUTFChars(env, filename_obj, filename);

        return ;
    }   

    int create = 1;

    struct stat path_stat;
    if(stat(filename, &path_stat)==0)
    {//path exists
        if(!S_ISREG(path_stat.st_mode))
        {
            jclass exp_cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
            (*env)->ThrowNew(env, exp_cls, "Path already exists but is not a file");

            //free
            (*env)->ReleaseStringUTFChars(env, filename_obj, filename);

            return ;
        }else
        {
            create = 0;
        }
        
    }


    int fd = open(filename, O_RDWR | (create==0?0:O_CREAT),
		  S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH);
    
    if (fd<0) 
    {
        char tmp[BUFFER_SIZE];

        char buffer[ERROR_MAX_SIZE];
        char * string  = strerror_r(errno, buffer, ERROR_MAX_SIZE);

        snprintf(tmp, BUFFER_SIZE, "Cannot open/create the file\nError (errno=%d) : %s\n",  errno, string);

        jclass exp_cls = (*env)->FindClass(env, "java/lang/IOException");
        (*env)->ThrowNew(env, exp_cls, tmp);

        //free
        (*env)->ReleaseStringUTFChars(env, filename_obj, filename);
        
        return ;
    }

    int allocation = posix_fallocate(fd, offset, length);
    if(allocation<0)
    {
        char tmp[BUFFER_SIZE];

        char buffer[ERROR_MAX_SIZE];
        char * string  = strerror_r(errno, buffer, ERROR_MAX_SIZE);

        snprintf(tmp, BUFFER_SIZE, "Cannot allocate disk space for the file\nError (errno=%d) : %s\n",  errno, string);

        jclass exp_cls = (*env)->FindClass(env, "java/lang/IOException");
        (*env)->ThrowNew(env, exp_cls, tmp);


        //free
        (*env)->ReleaseStringUTFChars(env, filename_obj, filename);

        return ;
    }

    if(close(fd)<0)
    {
        char tmp[BUFFER_SIZE];

        char buffer[ERROR_MAX_SIZE];
        char * string  = strerror_r(errno, buffer, ERROR_MAX_SIZE);

        snprintf(tmp, BUFFER_SIZE, "Cannot close the file\nError (errno=%d) : %s\n",  errno, string);

        jclass exp_cls = (*env)->FindClass(env, "java/lang/IOException");
        (*env)->ThrowNew(env, exp_cls, tmp);


        //free
        (*env)->ReleaseStringUTFChars(env, filename_obj, filename);

        return ;
    }

    //free
    (*env)->ReleaseStringUTFChars(env, filename_obj, filename);
}