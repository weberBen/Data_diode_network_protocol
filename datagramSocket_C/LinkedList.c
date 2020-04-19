#include <stdio.h> 
#include <stdlib.h> 

#include "LinkedList.h"


void freeNodeLinkedList(Node * node)
{
    if(node==NULL)
        return ;
    
    if(node->freeFunction!=NULL)
    {
        if(node->data!=NULL)
        {
            node->freeFunction(node->data);
        }
    }

    free(node);
}

void * freeNodeStructLinkedList(Node * node)
{
    if(node==NULL)
        return NULL;
    
    void * data = node->data;
    node->data = NULL;
    freeNodeLinkedList(node);

    return data;
}

Node * createNodeLinkedList(short int type, void *data, void (*freeFunction)(void *))
{
    Node * node = (Node *)malloc(sizeof(Node));
    if(node==NULL)
        return NULL;

    node->type = type;
    node->data = data;
    node->freeFunction = freeFunction;
    node->next = NULL;

    return node;
}

Node * popNodeLinkedList(LinkedList * linked_list)
{
    pthread_mutex_lock(&linked_list->lock_consumers);

    Node * node = NULL;
    if(linked_list->last==linked_list->first) 
    {
        if(linked_list->first==NULL)
        {
            pthread_mutex_unlock(&linked_list->lock_consumers);
            return NULL;
        }

        pthread_mutex_lock(&linked_list->lock_producer);
        
        if(linked_list->last==linked_list->first)//check if the two are still equals
        {
            node = linked_list->first;
            node->next = NULL;
            linked_list->first = NULL;
            linked_list->last = NULL;

        }else
        {
            node = linked_list->first;
            linked_list->first = node->next;
            node->next = NULL;
        }

        pthread_mutex_unlock(&linked_list->lock_producer);

    }else
    {
        node = linked_list->first;
        if(node!=NULL)
        {
            linked_list->first = node->next;
            node->next = NULL;
        }
    }
    
    pthread_mutex_unlock(&linked_list->lock_consumers);

    return node;
}

void * popDataLinkedList(LinkedList * linked_list)
{
    Node * node = popNodeLinkedList(linked_list);
    void * data = freeNodeStructLinkedList(node);

    return data;
}

LinkedList * createLinkedList(Error * error)
{
    LinkedList * linked_list= (LinkedList *)malloc(sizeof(LinkedList));
    if(linked_list==NULL)
    {
        setError(error, OUT_OF_MEMORY);
        return NULL;
    }

    linked_list->first = NULL;
    linked_list->last = NULL;

    int rc = pthread_mutex_init(&linked_list->lock_producer, NULL);
    if(rc!= 0)
    {
        char str[100];
        snprintf(str, sizeof(str), "Cannot init producer mutex (pthread_mutex_init) :\n\terrno=%d", rc);
        setError(error, str);

        free(linked_list);

        return NULL;
    }

    rc = pthread_mutex_init(&linked_list->lock_consumers, NULL);
    if(rc!= 0)
    {
        char str[100];
        snprintf(str, sizeof(str), "Cannot init consumer mutex (pthread_mutex_init) :\n\terrno=%d", rc);
        setError(error, str);

        free(linked_list);

        return NULL;
    }
    
    return linked_list;
}

void clearLinkedList(LinkedList * linked_list)
{
    if(linked_list==NULL)
        return ;

    pthread_mutex_lock(&linked_list->lock_producer);
    pthread_mutex_lock(&linked_list->lock_consumers);
    
    Node * node = linked_list->first;
    Node * next = NULL;
    while(node!=NULL)
    {
        next = node->next;
        freeNodeLinkedList(node);
        node = next;
    }

    linked_list->first = NULL;
    linked_list->last = NULL;

    pthread_mutex_unlock(&linked_list->lock_producer);
    pthread_mutex_unlock(&linked_list->lock_consumers);
}

void freeLinkedList(LinkedList * linked_list)
{
    clearLinkedList(linked_list);

    pthread_mutex_destroy(&linked_list->lock_producer);
    pthread_mutex_destroy(&linked_list->lock_consumers);

    free(linked_list);
}

void addLinkedList(LinkedList * linked_list, Node * node)
{
    int lock;
    
    if(linked_list->last==linked_list->first) 
    {
        lock = 1;
    }else
    {
        lock = 0;
    }
    

    if(lock)
        pthread_mutex_lock(&linked_list->lock_producer);
    
    if(linked_list->first==NULL)
    {
        linked_list->first = node;
    }else
    {
        linked_list->last->next = node;
    }
    linked_list->last = node;
    node->next = NULL;

    if(lock)
        pthread_mutex_unlock(&linked_list->lock_producer);
}
