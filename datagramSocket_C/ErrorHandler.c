#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <string.h>

#include "ErrorHandler.h"

#define MAX_CHAR 1000


void _freeErrorContent(_error * error)
{
    if(error==NULL)
        return;
    
    free(error->msg);
    error->msg = NULL;
}

void _freeError(_error * error)
{ 
    if(error==NULL)
        return ;

    _freeErrorContent(error);
    free(error);
}


_error * _createError()
{
    _error * s_error  =(_error *)malloc(sizeof(_error));
    if(s_error!=NULL)
        s_error->msg = NULL;

    return s_error;
}

int _setError(_error * error, char * msg)
{
    if(error==NULL)
        return -1;

    _freeErrorContent(error);
    
    error->msg = strndup(msg, MAX_CHAR);

    return 0;
}

_error * _createAndInitError(char * msg)
{
    _error * s_error = _createError();
    if(s_error==NULL)
        return NULL;
    
    _setError(s_error, msg);

    return s_error;
}


void initError(Error * error)
{
    if(error==NULL)
        return ;
    
    *error = NULL;
}

int setError(Error * error, char * msg)
{
    if(error==NULL)
        return -1;

    _freeError(*error);
    
    *error = _createAndInitError(msg);
    if(*error==NULL)
        return -1;
    
    return 0;
}

void removeError(Error * error)
{
    if(error==NULL || *error==NULL)
        return ;
    
    _freeError(*error);
    *error = NULL;
}

char * getErrorMsg(Error * error)
{
    if(error==NULL)
        return NULL;
    
    if(*error==NULL)
        return NULL;
    
    return (*error)->msg;
}


int isDefaultError(Error * error)
{
    if(error==NULL)
        return 1;
    
    if(*error==NULL)
        return 1;

    return 0;
}