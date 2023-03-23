#include <time.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>        /* For mode constants */
#include <fcntl.h>           /* For O_* constants */

#include "err.h"


//some consts
#define MAX_REINDEERS 7
#define MAX_ELFS 6
#define MAX_GIFTS 5

#define NO_REINDEERS 3
#define NO_ELFS 4
#define NO_GIFTS 5

#define BUFFSIZE 3
#define LOOPS 5

/**************************************************************************************************/
//storage compartment
struct storage{
    int buffer[BUFFSIZE];
    int take;
    int put;
    int total_collected;
    sem_t gifts;
    sem_t free_space;
    sem_t mutex;
};

void init(struct storage* s) {
    s->take = 0;
    s->put = 0;
    s->total_collected = 0;
    if (sem_init(&s->gifts, 1, 0))
        syserr("sem_init : gifts");
    if (sem_init(&s->free_space, 1, BUFFSIZE))
        syserr("sem_init : free_space");
    if (sem_init(&s->mutex, 1, 1))
        syserr("sem_init : mutex");
}

void end(struct storage* s) {
    sem_destroy(&s->gifts);
    sem_destroy(&s->free_space);
    sem_destroy(&s->mutex);
}

void insert(struct storage* s, int gift) {
    s->buffer[s->put] = gift;
    s->put = (s->put + 1) % BUFFSIZE;
}

int collect(struct storage* s) {
    int gift = s->buffer[s->take];
    s->take = (s->take + 1) % BUFFSIZE;
    s->total_collected = s->total_collected + 1;
    return gift;
}

/**************************************************************************************************/
//popular names
char *elfs_names[MAX_ELFS] = {"Mirek","Zuzia","Gienia", "Macius", "Ela", "Stasia"};
char *reindeers_names[MAX_REINDEERS] = {"Janek", "Zosia", "Franek", "Jozek", "Asia", "Olek", "Ruda"};
char *gifts[MAX_GIFTS] = {"lalka", "klocki", "ciuchcia", "rozga", "rower"};

/**************************************************************************************************/
//toymaker
int produce(){
  sleep(rand() % 3);
  return rand()%NO_GIFTS;
}

//sent to santa
void deliver(int i){
  sleep(rand() % 3);
}

void nap(int i){
  sleep(i);
}
/**************************************************************************************************/
//life of an elf
void elf(int id, struct storage* s){

  int i,g;
  printf("Hej! Jestem elfem o imieniu %s, zaczynam!\n", elfs_names[id]);
  for(i = 0; i< LOOPS; ++i){

    g = produce();
    printf("Hej! Jestem elfem o imieniu %s, wyprodukowalem/am prezent: %s\n", elfs_names[id], gifts[g]);

    if (sem_wait(&s->free_space))
        syserr("sem_wait : free_space");
    if (sem_wait(&s->mutex))
        syserr("sem_wait : mutex");
    insert(s, g);
    if (sem_post(&s->mutex))
        syserr("sem_post : mutex");
    if (sem_post(&s->gifts))
        syserr("sem_post : gifts");
    printf("Hej! Jestem elfem o imieniu %s, wstawilem/am prezent: %s\n", elfs_names[id], gifts[g]);
  }
}

/**************************************************************************************************/
//life of a reindeer
void reindeer(int id, struct storage* s){
  
  int end = 0;
  int g;
  
  printf("Hej! Jestem reniferem o imieniu %s, zaczynam!\n", reindeers_names[id]);

  if (id >= NO_ELFS * LOOPS) {
      return; // we only need NO_ELFS * LOOPS reindeers
  }

  while(!end){
    if (sem_wait(&s->gifts))
        syserr("sem_wait : gifts");
    if (sem_wait(&s->mutex))
        syserr("sem_wait : mutex");
    g = collect(s);
    if (s->total_collected > NO_ELFS * LOOPS - NO_REINDEERS) {
        end = 1;
    }
    if (sem_post(&s->mutex))
        syserr("sem_post : mutex");
    if (sem_post(&s->free_space))
        syserr("sem_post : free_space");
    printf("Hej! Jestem reniferem o imieniu %s, odebralem/am prezent: %s\n", reindeers_names[id], gifts[g]);

    deliver(g);
    printf("Hej! Jestem reniferem o imieniu %s, dostarczylem/am prezent: %s\n", reindeers_names[id], gifts[g]);
  }
}
/**************************************************************************************************/
/**************************************************************************************************/
int main(){
    
  int i;
  pid_t pid;
  struct storage *mapped_mem;

  mapped_mem = mmap(
    NULL,
    sizeof (struct storage),
    PROT_READ | PROT_WRITE,
    MAP_SHARED | MAP_ANONYMOUS,
    -1,
    0);

  if (mapped_mem == MAP_FAILED)
      syserr("mmap");

  init(mapped_mem);

  int seed = time(0);
  srand(seed);

  printf("Tworze pracownikow.\nElfy: %d; Renifery: %d\n", NO_ELFS, NO_REINDEERS);
  
  for(i = 0; i < NO_ELFS + NO_REINDEERS; i++){
    
    rand();//some randomness
    switch(pid = fork()){
    case -1:
      syserr("fork");
    case 0:
      srand(rand());
      if (i < NO_ELFS){
        printf("Elf %d!\n", i);
        elf(i, mapped_mem);
        
      }else{
        printf("Renifer %d!\n", i);
        reindeer(i - NO_ELFS,mapped_mem);
      }
      return 0;
    default:
      nap(1);
      printf("Kolejny pracownik!\n");
      break;
    }
  }

  for(i = 0; i< NO_ELFS+NO_REINDEERS; ++i) wait(0);

  end(mapped_mem);

  return 0;
  }
