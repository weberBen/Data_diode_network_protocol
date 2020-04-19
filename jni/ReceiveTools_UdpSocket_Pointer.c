#include <stdio.h> 
#include <stdlib.h>
#include <string.h>

#include "ReceiveTools_UdpSocket.h" 
#include "datagramReceiver.h"

#define POINTER_CLASS_NAME "ReceiveTools/Pointer"

int setPointerAddress(JNIEnv *env, jobject ptr_obj, long new_value)
{
    jclass cls = (*env)->FindClass(env,POINTER_CLASS_NAME);
    if(cls==NULL)
        return -1;
    
    jfieldID fid = (*env)->GetFieldID(env, cls , "address", "J");
    (*env)->SetLongField(env, ptr_obj, fid, (jlong)new_value);
    
    return 0;
}

int getPointerAddress(JNIEnv *env, jobject ptr_obj, long *actual_value)
{
    jclass cls = (*env)->FindClass(env,POINTER_CLASS_NAME);
    if(cls==NULL)
        return -1;

    jfieldID fid = (*env)->GetFieldID(env, cls , "address", "J");
    jlong value = (*env)->GetLongField(env, ptr_obj, fid);

    *actual_value = (long)value;
    return 0;
}

jobject createJavaPointer(JNIEnv *env, void * ptr)
{
    jclass cls = (*env)->FindClass(env,POINTER_CLASS_NAME);
    if(cls==NULL)
        return NULL;
    
    jmethodID mdID = (*env)->GetMethodID(env,cls,"<init>","()V");
    if(mdID==NULL)
        return NULL;

    jobject new_obj = (*env)->NewObject(env, cls, mdID);
    if(new_obj==NULL)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, exp_cls, "Out of Memory");

        return NULL;
    }

    if(setPointerAddress(env, new_obj, (long)ptr)==-1)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/IllegalAccessException");
        (*env)->ThrowNew(env, exp_cls, "Cannot set pointer address");

        return NULL;
    }

    return new_obj;
}


void * getPointer(JNIEnv *env, jobject pointer)
{
    long address;
    if(getPointerAddress(env, pointer, &address)==-1)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/IllegalAccessException");
        (*env)->ThrowNew(env, exp_cls, "Cannot get the pointer adress");

        return NULL;
    }
    
    return (void *)address;
}

JNIEXPORT jobject JNICALL Java_ReceiveTools_UdpSocket_openUdpSocket(JNIEnv *env, jobject obj, 
    jstring hostname_obj, jint port_obj, jint max_buffer_size_obj) 
{  

    const char * hostname = (*env)->GetStringUTFChars(env, hostname_obj, NULL);
    if(hostname==NULL)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/Exception");
        (*env)->ThrowNew(env, exp_cls, "Ip address is NULL");

        //free
        (*env)->ReleaseStringUTFChars(env, hostname_obj, hostname);

        return NULL;
    }
    
    int port = (int)port_obj;
    if(port<0)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/Exception");
        (*env)->ThrowNew(env, exp_cls, "Ip port can not be negative");

        //free
        (*env)->ReleaseStringUTFChars(env, hostname_obj, hostname);

        return NULL;
    }
    int max_buffer_size = (int)max_buffer_size_obj;
    if(max_buffer_size<0)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/Exception");
        (*env)->ThrowNew(env, exp_cls, "Buffer size can not be negative");

        //free
        (*env)->ReleaseStringUTFChars(env, hostname_obj, hostname);

        return NULL;
    }

    printf("port=%d, buffer_size=%d, hostname=%s\n", port, max_buffer_size, hostname);
    //create pointer obj
    Error error;initError(&error);
    SocketInstance * socket_instance = openUdpSocket(&error, hostname, (unsigned short int)port, (unsigned int)max_buffer_size);
    if(socket_instance==NULL)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/InterruptedException");
        (*env)->ThrowNew(env, exp_cls, getErrorMsg(&error));

        //free
        (*env)->ReleaseStringUTFChars(env, hostname_obj, hostname);
        removeError(&error);

        return NULL;
    }
    
    //free
    (*env)->ReleaseStringUTFChars(env, hostname_obj, hostname);
    removeError(&error);

    jobject prt = createJavaPointer(env, (void *)socket_instance);

    return prt;
}

jbyteArray toJByteArray(JNIEnv *env, char * buffer, unsigned int buffer_size)
{
    jbyteArray jbyte_array = (*env)->NewByteArray(env, buffer_size);
    if (jbyte_array == NULL) 
    {
        return NULL; //  out of memory error thrown
    }
    
    (*env)->SetByteArrayRegion(env, jbyte_array, 0, buffer_size, (jbyte*)buffer);

    // creat bytes from byteUrl
    /*jbyte * bytes = (*env)->GetPrimitiveArrayCritical(env, jbyte_array, NULL);
    if(bytes==NULL)
    {
        (*env)->ReleasePrimitiveArrayCritical(env, jbyte_array, bytes, 0);

        return NULL;
    }

    int i;
    for(i = 0; i < buffer_size; i++) 
    {
        bytes[i] = buffer[i];
    }

    (*env)->ReleasePrimitiveArrayCritical(env, jbyte_array, bytes, 0);*/

    return jbyte_array;
}

JNIEXPORT void JNICALL Java_ReceiveTools_UdpSocket_dropAllUdpReceivedPackets(JNIEnv *env, jobject obj, jobject pointer)
{
    void * ptr = getPointer(env, pointer);
    if(ptr==NULL)
        return ;
    
    SocketInstance * socket_instance = (SocketInstance *)ptr;
    if(socket_instance->error!=NULL)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/InterruptedException");
        (*env)->ThrowNew(env, exp_cls, getErrorMsg(&socket_instance->error));

        return ;
    }

    dropAllReceivedPackets(socket_instance);
}

JNIEXPORT jbyteArray JNICALL Java_ReceiveTools_UdpSocket_getUdpReceivedPacket(JNIEnv *env, jobject obj, jobject pointer)
{
    /*long ptr = (long)ptr_address;
    char * buffer = (char *)ptr;
    free(buffer);*/

    void * ptr = getPointer(env, pointer);
    if(ptr==NULL)
        return NULL;
    
    SocketInstance * socket_instance = (SocketInstance *)ptr;
    if(!isDefaultError(&socket_instance->error))
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/InterruptedException");
        (*env)->ThrowNew(env, exp_cls, getErrorMsg(&socket_instance->error));

        return NULL;
    }

    CharArray * char_array = getUdpReceivedPacket(socket_instance);
    if(char_array==NULL)
        return NULL;
    
    jbyteArray jbyte_array = toJByteArray(env, char_array->array, char_array->length);
    if(jbyte_array==NULL)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, exp_cls, "Out of Memory");
    }

    freeCharArray(char_array);

    return jbyte_array;
}


JNIEXPORT void JNICALL Java_ReceiveTools_UdpSocket_closeUdpSocket(JNIEnv *env, jobject obj, jobject pointer)
{
    if(pointer==NULL)
        return ;
    
    void * ptr = getPointer(env, pointer);
    if(ptr==NULL)
    {
        printf("null pointer !\n");
        return ;
    }

    SocketInstance * socket_instance = (SocketInstance *)ptr;

    Error error;initError(&error);
    if(closeUdpSocket(&error, socket_instance)==-1)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/InterruptedException");
        (*env)->ThrowNew(env, exp_cls, error->msg);
        removeError(&error);
    }
    fprintf(stderr, "close close close");
    //set pointer to null
    if(setPointerAddress(env, pointer, (long)NULL)==-1)
    {
        jclass exp_cls = (*env)->FindClass(env, "java/lang/IllegalAccessException");
        (*env)->ThrowNew(env, exp_cls, "Cannot set the pointer adress");
    }

    removeError(&error);
}