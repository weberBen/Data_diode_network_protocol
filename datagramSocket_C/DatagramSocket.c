#include <stdio.h> 
#include <stdlib.h> 
#include <unistd.h> 
#include <string.h> 
#include <sys/types.h> 
#include <sys/socket.h> 
#include <arpa/inet.h> 
#include <netinet/in.h> 
#include <netdb.h> 
#include <pthread.h>
#include <time.h>
#include <errno.h>
#include <signal.h>  
#include <assert.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sched.h>

#include "DatagramSocket.h"

#define MAX_IP_PORT 65535
#define MAX_ERR_MSG_LENGTH 500
#define CHAR_ARRAY_LINKED_LIST_TYPE 1


void * startListening(void * parms);

CharArray * getUdpReceivedPacket(SocketInstance * socket_instance)
{
    if(socket_instance==NULL || socket_instance->linked_list==NULL)
        return NULL;

    CharArray * char_array = (CharArray *)popDataLinkedList(socket_instance->linked_list);
    return char_array;
}

void dropAllReceivedPackets(SocketInstance * socket_instance)
{
    if(socket_instance==NULL || socket_instance->linked_list==NULL)
        return ;

    clearLinkedList(socket_instance->linked_list);
}

SocketInstance * openUdpSocket(Error * error, const char * hostname, unsigned short int port, unsigned int max_buffer_size)
{
    Error inner_error; initError(&inner_error);

    if(max_buffer_size==0)
    {
        setError(error, "The buffer size must be >0");

        removeError(&inner_error);
        return NULL;
    }

    // Getting network parms
    if(port>MAX_IP_PORT)
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "IP port <<%d>> is not valid", port);
        setError(error, str);

        removeError(&inner_error);

        return NULL;
    }

    struct hostent *hostinfo = gethostbyname(hostname); /* on récupère les informations de l'hôte auquel on veut se connecter */
    if (hostinfo == NULL) /* l'hôte n'existe pas */
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "IP address <<%s>> is not valid", hostname);
        setError(error, str);

        removeError(&inner_error);

        return NULL;
    }

    // Filling server information 
    struct sockaddr_in  * servaddr = (struct sockaddr_in *)malloc(sizeof(struct sockaddr_in));
    if(servaddr==NULL)
    {
        setError(error, OUT_OF_MEMORY);

        removeError(&inner_error);

        return NULL;
    }
    memset(servaddr, 0, sizeof(* servaddr)); 

    servaddr->sin_family = hostinfo->h_addrtype;
    servaddr->sin_addr = *(struct in_addr *) hostinfo->h_addr; 
    servaddr->sin_port = htons(port); 

    // Starting socket
    int socket_fd = socket(hostinfo->h_addrtype, SOCK_DGRAM, 0);
    if(socket_fd==-1) 
    { 
        //error handling
        char str[MAX_ERR_MSG_LENGTH];

        char str_errno[MAX_ERR_MSG_LENGTH];
        strerror_r(errno, str_errno, sizeof(str_errno));

        snprintf(str, sizeof(str), "UDP socket creation failure : \n\terrno=%d\n\t%s", errno, str_errno);
        setError(error, str);

        //free memory
        free(servaddr);
        removeError(&inner_error);
        
        return NULL;
    }

    //set the socket reusable
    int yes=1;
    if(setsockopt(socket_fd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(yes))==-1) 
    {
        char str[MAX_ERR_MSG_LENGTH];

        char str_errno[MAX_ERR_MSG_LENGTH];
        strerror_r(errno, str_errno, sizeof(str_errno));

        snprintf(str, sizeof(str), "Connot set the socket as resuable : \n\terrno=%d\n\t%s", errno, str_errno);
        setError(error, str);

        //free memory
        free(servaddr);
        close(socket_fd);
        removeError(&inner_error);
        
        return NULL;
    }

    // Bind the socket with the server address 
    if (bind(socket_fd, (const struct sockaddr *)servaddr, sizeof(*servaddr))==-1) 
    { 
        char str[MAX_ERR_MSG_LENGTH];

        char str_errno[MAX_ERR_MSG_LENGTH];
        strerror_r(errno, str_errno, sizeof(str_errno));

        snprintf(str, sizeof(str), "Bind to socket failure : \n\terrno=%d\n\t%s", errno, str_errno);
        setError(error, str);

        //free memory
        free(servaddr);
        close(socket_fd);
        removeError(&inner_error);
        
        return NULL;
    }



    // Setting buffer
    CharArray * array = createCharArray(max_buffer_size);
    if(array==NULL)
    {
        setError(error, OUT_OF_MEMORY);

        free(servaddr);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }

    // Setting linked list
    LinkedList * linked_list = createLinkedList(&inner_error);
    if(linked_list==NULL)
    {
        char str[MAX_ERR_MSG_LENGTH];

        snprintf(str, sizeof(str), "Reception queue creation failure : \n\t%s", getErrorMsg(&inner_error));
        setError(error, str);

        free(servaddr);
        freeCharArray(array);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }

    SocketInstance * socket_instance = (SocketInstance *)malloc(sizeof(SocketInstance));
    if(socket_instance==NULL)
    {
        setError(error, OUT_OF_MEMORY);

        free(servaddr);
        freeCharArray(array);
        freeLinkedList(linked_list);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }

    socket_instance->socket_fd = socket_fd;
    socket_instance->servaddr = servaddr;
    socket_instance->buffer = array;
    socket_instance->linked_list = linked_list;
    initError(&socket_instance->error);
    socket_instance->canceled = 0;
    socket_instance->closed = 0;

    //starting thread
    pthread_t thread;
    pthread_attr_t thread_attr;
    int rc;

    rc = pthread_attr_init(&thread_attr);
    if(rc!=0)
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "Cannot init thread attributes (pthread_attr_init): \n\terrno=%d", rc);
        setError(error, str);

        free(servaddr);
        freeCharArray(array);
        freeLinkedList(linked_list);
        free(socket_instance);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }
    rc = pthread_attr_setdetachstate(&thread_attr,PTHREAD_CREATE_DETACHED);
    if(rc!=0)
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "Cannot set thread attributes (pthread_attr_setdetachstate): \n\terrno=%d", rc);
        setError(error, str);

        free(servaddr);
        freeCharArray(array);
        freeLinkedList(linked_list);
        free(socket_instance);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }

    
    struct sched_param param;
    rc = pthread_attr_getschedparam (&thread_attr, &param);
    if(rc!=0)
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "Cannot get thread attributes (pthread_attr_getschedparam): \n\terrno=%d", rc);
        setError(error, str);

        free(servaddr);
        freeCharArray(array);
        freeLinkedList(linked_list);
        free(socket_instance);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }

    rc = pthread_attr_setinheritsched(&thread_attr,PTHREAD_EXPLICIT_SCHED);
    if(rc!=0)
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "Cannot set thread attributes (pthread_attr_setinheritsched): \n\terrno=%d", rc);
        setError(error, str);

        free(servaddr);
        freeCharArray(array);
        freeLinkedList(linked_list);
        free(socket_instance);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }


    rc = pthread_attr_setschedpolicy(&thread_attr, SCHED_RR);
    if(rc!=0)
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "Cannot set thread attributes (pthread_attr_setschedpolicy): \n\terrno=%d", rc);
        setError(error, str);

        free(servaddr);
        freeCharArray(array);
        freeLinkedList(linked_list);
        free(socket_instance);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }

    param.sched_priority = sched_get_priority_max(SCHED_RR);
    rc = pthread_attr_setschedparam(&thread_attr, &param);
    if(rc!=0)
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "Cannot set thread attributes (pthread_attr_setschedparam): \n\terrno=%d", rc);
        setError(error, str);

        free(servaddr);
        freeCharArray(array);
        freeLinkedList(linked_list);
        free(socket_instance);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }

    if ((rc = pthread_create(&thread, &thread_attr, startListening, socket_instance))!=0) 
    {
        char str[MAX_ERR_MSG_LENGTH];
        snprintf(str, sizeof(str), "Start new thread for reception failure : \n\terrno=%d", rc);
        setError(error, str);

        free(servaddr);
        freeCharArray(array);
        freeLinkedList(linked_list);
        free(socket_instance);
        close(socket_fd);
        removeError(&inner_error);

        return NULL;
    }
    socket_instance->thread = thread;

    removeError(&inner_error);

    return socket_instance;

}

void freeFunctionLinkedList(void * data)
{
    CharArray * value = (CharArray *)data;
    freeCharArray(value);
}

int addCharArrayLinkedList(Error * error, LinkedList * linked_list, char * buffer, unsigned int length)
{
    CharArray * data = createAndInitCharArray(buffer, length);
    if(data==NULL)
    {
        setError(error, OUT_OF_MEMORY);

        return -1;
    }

    Node * node = createNodeLinkedList(CHAR_ARRAY_LINKED_LIST_TYPE, (void *)data, freeFunctionLinkedList);
    if(node==NULL)
    {
        setError(error, OUT_OF_MEMORY);
        freeCharArray(data);
        return -1;
    }

    addLinkedList(linked_list, node);

    return 0;
}


void * startListening(void * parms)
{

    SocketInstance * socket_instance = (SocketInstance *)parms;

    struct sockaddr_in  cliaddr;
    memset(&cliaddr, 0, sizeof(cliaddr)); 

    socklen_t len, packet_len; 
    len = sizeof(cliaddr);  //len is value/resuslt

    char * buffer = socket_instance->buffer->array;
    unsigned int buffer_size = socket_instance->buffer->length;
    int socket_fd = socket_instance->socket_fd;

    char * packet;
    long count = 0;

    Error error; initError(&error);

    fprintf(stderr, "waitings paquets\n");
    while(1)
    {
        packet_len = recvfrom(socket_fd, buffer, buffer_size,  
                    MSG_WAITALL, (struct sockaddr *)&cliaddr, 
                    &len);
        
        if(packet_len==0 || socket_instance->canceled)
            break;
        
        if(packet_len<0)
        {
            char str[MAX_ERR_MSG_LENGTH];

            char str_errno[MAX_ERR_MSG_LENGTH];
            strerror_r(errno, str_errno, sizeof(str_errno));

            snprintf(str, sizeof(str), "Reception failure : \n\terrno=%d\n\t%s", errno, str_errno);
            setError(&socket_instance->error, str);

            break;
        }
        
        packet = (char*)malloc(sizeof(char)*packet_len);
        if(packet==NULL)
        {
            setError(&socket_instance->error, OUT_OF_MEMORY);

            break;
        }
        
        //buffer has no '\0' at the end
        for(int i=0; i<packet_len; i++)
        {
            packet[i] = buffer[i];
        }

        if(addCharArrayLinkedList(&error, socket_instance->linked_list, packet, packet_len)<0)
        {
            char str[MAX_ERR_MSG_LENGTH];
            snprintf(str, sizeof(str), "Cannot add new packet to the reception queue : \n\t%s", getErrorMsg(&error));

            setError(&socket_instance->error, str);

            free(packet);

            break;
        }

        count++;
    }

    removeError(&error);
    socket_instance->closed = 1;

    fprintf(stderr, "total count written = %ld\n", count);

    fprintf(stderr, "end waitings paquets\n");

    return NULL;
}


void freeSocketInstance(SocketInstance * socket_instance)
{
    if(socket_instance==NULL)
        return ;

    free(socket_instance->servaddr);
    freeCharArray(socket_instance->buffer);
    freeLinkedList(socket_instance->linked_list);
    removeError(&socket_instance->error);

    free(socket_instance);
}

void closeReceptionThread(SocketInstance * socket_instance)
{
    socket_instance->canceled = 1;
    socklen_t len = sizeof(*(socket_instance->servaddr));

    while(!socket_instance->closed)
    {
        fprintf(stderr, "sendng void packet\n");
        sendto(socket_instance->socket_fd, NULL, 0,  0, (const struct sockaddr *)socket_instance->servaddr, len);
        usleep(1000*10);//10ms
    }
}

int closeUdpSocket(Error * error, SocketInstance * socket_instance)
{
    int err_flag = 0;

    if(socket_instance==NULL)
        return err_flag;

    closeReceptionThread(socket_instance);

    if(close(socket_instance->socket_fd)==-1)
    {
        err_flag = -1;

        char str[MAX_ERR_MSG_LENGTH];

        char str_errno[MAX_ERR_MSG_LENGTH];
        strerror_r(errno, str_errno, sizeof(str_errno));

        snprintf(str, sizeof(str), "Cannot close udp socket : \n\terrno=%d\n\t%s", errno, str_errno);
        setError(error, str);
    }
    freeSocketInstance(socket_instance);

    return err_flag;
}