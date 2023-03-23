#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include "err.h"

#define READERS 3
#define WRITERS 2
#define NAP 2
#define BSIZE 32

struct readwrite {
  pthread_mutex_t lock;         
  pthread_cond_t readers;      
  pthread_cond_t writers;    
  int rcount, wcount, rwait, wwait;
  int change;
};

struct readwrite library;
char book[BSIZE];
int working = 1;

/* Initialize a buffer */

void init(struct readwrite *rw) {
    int err;

    if ((err = pthread_mutex_init(&rw->lock, 0)) != 0)
        syserr (err, "mutex init failed");
    if ((err = pthread_cond_init(&rw->readers, 0)) != 0)
        syserr (err, "cond init 1 failed");
    if ((err = pthread_cond_init(&rw->writers, 0)) != 0)
        syserr (err, "cond init 2 failed");

    library.rcount = 0;
    library.wcount = 0;
    library.rwait = 0;
    library.wwait = 0;
}

/* Destroy the buffer */

void destroy(struct readwrite *rw) {
    int err;

    if ((err = pthread_cond_destroy (&rw->writers)) != 0)
        syserr (err, "cond destroy 1 failed");
    if ((err = pthread_cond_destroy (&rw->readers)) != 0)
        syserr (err, "cond destroy 2 failed");
    if ((err = pthread_mutex_destroy (&rw->lock)) != 0)
        syserr (err, "mutex destroy failed");
}

void *reader(void *data)
{
    int err;

    while (working) {

        if ((err = pthread_mutex_lock(&library.lock)) != 0)
            syserr (err, "lock failed");

        while (library.wcount + library.wwait > 0 && library.change == 1) {
            library.rwait++;

            if ((err = pthread_cond_wait(&library.readers, &library.lock)) != 0)
                syserr (err, "cond wait failed");

            library.rwait--;

        }

        library.rcount++;

        if (library.wwait == 0) {
            if ((err = pthread_cond_signal(&library.readers)) != 0)
                syserr (err, "cond signal failed");
        }

        if ((err = pthread_mutex_unlock(&library.lock)) != 0)
            syserr (err, "unlock failed");


        printf("reader read: %s\n", book); /* reading */


        if ((err = pthread_mutex_lock(&library.lock)) != 0)
            syserr (err, "lock failed");

        library.rcount--;
        library.change = 1;

        if (library.rcount == 0) {
            if ((err = pthread_cond_signal(&library.writers)) != 0)
                syserr (err, "cond signal failed");
        }

        if ((err = pthread_mutex_unlock(&library.lock)) != 0)
            syserr (err, "unlock failed");
    }

    return 0;
}

void *writer(void *data)
{
    int err;
    int l;
    while (working) {

        if ((err = pthread_mutex_lock(&library.lock)) != 0)
            syserr (err, "lock failed");

        while (library.wcount + library.rcount > 0 || (library.rwait > 0 && library.change == 0)) {
            library.wwait++;

            if ((err = pthread_cond_wait(&library.writers, &library.lock)) != 0)
                syserr (err, "cond wait failed");

            library.wwait--;
        }


        library.wcount++;

        if ((err = pthread_mutex_unlock(&library.lock)) != 0)
            syserr (err, "unlock failed");


        l = rand()%10;
        snprintf(book, BSIZE, "6 times a number %d %d %d %d %d %d", l, l, l, l, l, l);


        if ((err = pthread_mutex_lock(&library.lock)) != 0)
            syserr (err, "lock failed");

        library.wcount--;
        library.change = 0;

        if (library.rwait > 0) {
            if ((err = pthread_cond_signal(&library.readers)) != 0)
                syserr (err, "cond signal failed");
        }
        else {
            if ((err = pthread_cond_signal(&library.writers)) != 0)
                syserr (err, "cond signal failed");
        }
        if ((err = pthread_mutex_unlock(&library.lock)) != 0)
            syserr (err, "unlock failed");
    }

    return 0;
}


int main() {
  pthread_t th[READERS+WRITERS];
  pthread_attr_t attr;
  int i, err;
  void *retval;

  srand((unsigned)time(0));
  
  init(&library);
  if ((err = pthread_attr_init (&attr)) != 0)
    syserr (err, "attr_init failed");
  if ((err = pthread_attr_setdetachstate (&attr,PTHREAD_CREATE_JOINABLE)) != 0)
    syserr (err, "attr_setdetachstate failed");

  for (i = 0; i < READERS + WRITERS; i++) {
    if (i < READERS) {
      if ((err = pthread_create(&th[i], &attr, reader, 0)) != 0)
	syserr (err, "create failed");
    } else 
      if ((err = pthread_create(&th[i], &attr, writer, 0)) != 0)
	syserr (err, "create failed");
  }
  
  sleep(NAP);
  working = 0;

  for (i = 0; i < READERS + WRITERS; i++) {
    if ((err = pthread_join(th[i], &retval)) != 0)
    syserr (err, "join failed");
  }
  
  if ((err = pthread_attr_destroy (&attr)) != 0)
    syserr (err, "cond destroy failed");
  destroy(&library);
  return 0;
}
