#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "DatagramSocket.h"
#include "LinkedList.h"
#include "CharArray.h"
#include "ErrorHandler.h"


void freeFunction1(void * data)
{
    char * value = (char * )data;
    free(value);
}
long stop = 0;

void * thread1(void * args)
{
    LinkedList * list = (LinkedList *)args;

    char * buffer;
    Node * node;

    for(int i=0; i<10000; i++)
    {
        printf( "threa 1 send %d\n", i);
        buffer = (char *)malloc(sizeof(char)*10);
        node = createNodeLinkedList(1, (void *)buffer, freeFunction1);
        addLinkedList(list, node);
    }

    stop = 1;

    return NULL;
}

void * thread2(void * args)
{
    LinkedList * list = (LinkedList *)args;

    Node * node;
    long count = 0;
    while(!stop)
    {
        node = popNodeLinkedList(list);
        if(node==NULL)
            continue;
        printf("type pop=%d\n", node->type);
        count++;
        freeNodeLinkedList(node);
    }

    node = popNodeLinkedList(list);
    while(node!=NULL)
    {
        printf("type pop=%d\n", node->type);
        count++;
        freeNodeLinkedList(node);
        node = popNodeLinkedList(list);
    }

    printf("----------------coutn value=%ld\n", count);
    return NULL;
}

void * thread3(void * args)
{
    printf("****** clear list\n");
    LinkedList * list = (LinkedList *)args;

    clearLinkedList(list);

    return NULL;
}


long stop1 = 0;
void * gather(void * data)
{
    long count = 0;
    SocketInstance * socket_instance = (SocketInstance *)data;
    while(!stop1)
    {
        CharArray * buffer = getUdpReceivedPacket(socket_instance);
        if(buffer==NULL)
            continue;
        
        count++;
        freeCharArray(buffer);
        usleep(100);
    }

    CharArray * buffer = getUdpReceivedPacket(socket_instance);
    while(buffer!=NULL)
    {
        freeCharArray(buffer);
        count++;
        buffer = getUdpReceivedPacket(socket_instance);
    }

    fprintf(stderr, "total count read = %ld\n", count);


    return NULL;
}

int main()
{
    Error error1; initError(&error1);
    LinkedList * list = createLinkedList(&error1);


    pthread_t t11, t22, t33;
    int rrrc = pthread_create(&t11, NULL, thread1, list);
    if (rrrc) 
    {
         printf("Error:unable to create thread 2, %d\n", rrrc);
    }

    rrrc = pthread_create(&t22, NULL, thread2, list);
    if (rrrc) 
    {
         printf("Error:unable to create thread 2, %d\n", rrrc);
    }

    char invoer1;
    printf("enter : ");
    scanf("%c",&invoer1); 
    printf("end : ");

    rrrc = pthread_create(&t33, NULL, thread3, list);
    if (rrrc) 
    {
         printf("Error:unable to create thread 3, %d\n", rrrc);
    }

    pthread_join(t11, NULL);
    pthread_join(t22, NULL);
    pthread_join(t33, NULL);

    freeLinkedList(list);
    removeError(&error1);

    return 0;

    Error error; initError(&error);

    SocketInstance * socket_instance = openUdpSocket(&error, "169.254.8.186", 1652, 1500);
    if(socket_instance==NULL)
    {
        fprintf(stderr, "Error :\n%s", getErrorMsg(&error));
        removeError(&error);
        exit(-1);
    }

    pthread_t t1, t2;
    int rrc = pthread_create(&t1, NULL, gather, socket_instance);
    if (rrc) 
    {
         printf("Error:unable to create thread 2, %d\n", rrc);
         removeError(&error);
         exit(-1);
    }

    rrc = pthread_create(&t2, NULL, gather, socket_instance);
    if (rrc) 
    {
         printf("Error:unable to create thread 2, %d\n", rrc);
         removeError(&error);
         exit(-1);
    }

    

    char invoer;
    printf("enter : ");
    scanf("%c",&invoer); 
    printf("end : ");

    stop1 = 1;
    pthread_join(t1, NULL);
    pthread_join(t2, NULL);

    if(socket_instance->error!=NULL)
    {
        fprintf(stderr, "AFTER Error :\n%s", getErrorMsg(&socket_instance->error));
    }
    if(closeUdpSocket(&error, socket_instance)==-1)
    {
        fprintf(stderr, "Error :\n%s", getErrorMsg(&error));
        removeError(&error);
        exit(-1);
    }

    
    removeError(&error);

    fprintf(stderr, "end main\n");

    return 0;
}