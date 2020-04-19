#ifndef H_DATAGRAM_SOCKET
#define H_DATAGRAM_SOCKET

#include "LinkedList.h"
#include "CharArray.h"
#include "ErrorHandler.h"


typedef struct socketInstance
{
    pthread_t thread;
    int socket_fd;
    int canceled;
    int closed;
    struct sockaddr_in * servaddr;
    CharArray * buffer;
    LinkedList * linked_list;
    Error error;

} SocketInstance;


SocketInstance * openUdpSocket(Error * error, const char * hostname, unsigned short int port, unsigned int max_buffer_size);
CharArray * getUdpReceivedPacket(SocketInstance * socket_instance);
void dropAllReceivedPackets(SocketInstance * socket_instance);
int closeUdpSocket(Error * error, SocketInstance * socket_instance);


#endif