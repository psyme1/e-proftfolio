#include "rcv.h"
int main(int argc, char *argv[]){
    if(argc != 2 && argc != 4) return 1;
    tally_t *t;
    if(argc == 2){
        t = tally_from_file(argv[1]);
    }
    else{
        LOG_LEVEL = atoi(argv[2]);
        t = tally_from_file(argv[3]);
    }
    if(t == NULL){ 
        printf("Could not load votes file. Exiting with error code 1\n"); 
        return 1;
    }
    tally_election(t);
    tally_free(t);
    return 0;
}