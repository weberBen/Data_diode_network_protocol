#ifndef H_DATAGRAM_RECEIVER
#define H_DATAGRAM_RECEIVER

#include "LinkedList.h"
#include "CharArray.h"
#include "ErrorHandler.h"
#include "DatagramSocket.h"

extern SocketInstance * openUdpSocket(Error * error, const char * hostname, unsigned short int port, unsigned int max_buffer_size);
extern CharArray * getUdpReceivedPacket(SocketInstance * socket_instance);
extern int closeUdpSocket(Error * error, SocketInstance * socket_instance);
void dropAllReceivedPackets(SocketInstance * socket_instance);
extern void freeCharArray(CharArray * char_array);

#endif
