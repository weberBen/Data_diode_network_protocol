#ifndef H_LINKED_LIST
#define H_LINKED_LIST

#include<pthread.h>

#include "ErrorHandler.h"

typedef struct _node Node;

struct _node
{
    short int type;
    void * data;
    void (*freeFunction)(void *);
    Node * next;
};

typedef struct linked_list
{
    Node * first;
    Node * last;
    pthread_mutex_t lock_producer;
    pthread_mutex_t lock_consumers;
} LinkedList;

void freeNodeLinkedList(Node * node);
Node * createNodeLinkedList(short int type, void *data, void (*freeFunction)(void *));
Node * popNodeLinkedList(LinkedList * linked_list);
void * popDataLinkedList(LinkedList * linked_list);
LinkedList * createLinkedList(Error * error);
void freeLinkedList(LinkedList * linked_list);
void addLinkedList(LinkedList * linked_list, Node * node);
void clearLinkedList(LinkedList * linked_list);

#endif