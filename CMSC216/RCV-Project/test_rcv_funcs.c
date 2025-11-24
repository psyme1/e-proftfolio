#include "rcv.h"

// macro to set up a test with given name, print the source of the
// test; very hacky, fragile, but useful
#define IF_TEST(TNAME) \
  if( RUNALL || strcmp( TNAME, test_name)==0 ) { \
    tally_reset(tally); LOG_LEVEL=0;                                    \
    sprintf(sysbuf,"awk 'NR==(%d){P=1;gsub(\"^ *\",\"\");} P==1 && /ENDTEST/{P=0; print \"}\\n---OUTPUT---\"} P==1{print}' %s", __LINE__, __FILE__); \
    system(sysbuf); nrun++;  \
  } \
  if( RUNALL || strcmp( TNAME, test_name)==0 )

char sysbuf[1024];
int RUNALL = 0;
int nrun = 0;

// testing function to build up a tally in a
// visually easy to understand format
void tally_add(tally_t *t, char *name, int status, int vote_count){
  int idx = t->candidate_count;
  t->candidate_count++;
  strcpy(t->candidate_names[idx], name);
  t->candidate_status[idx] = status;
  t->candidate_vote_counts[idx] = vote_count;
}

// reset all fields of the tally to 0
void tally_reset(tally_t *tally){
  memset(tally, 0, sizeof(tally_t));
}

// convert state to a string for easy display
char *condition2str(int i){
  if(i < 0 || i > 4){
    return "UNKNOWN";
  }
  char *cond_strs[] = {
    "ZERO",
    "TALLY_ERROR",
    "TALLY_WINNER",
    "TALLY_TIE",
    "TALLY_CONTINUE",
  };
  return cond_strs[i];
}

// convenience function to make test votes
vote_t *vote_make(int id, int pos, ...){
  vote_t *v = vote_make_empty();
  v->id = id;
  v->pos = pos;
  v->next = NULL;
  va_list ptr;
  va_start(ptr, pos);
  for(int i=0; ; i++){
    int cand = va_arg(ptr,int);
    if(cand == NO_CANDIDATE){
      break;
    }
    v->candidate_order[i] = cand;
  }
  return v;
}

int main(int argc, char *argv[]){
  if(argc < 2){
    printf("usage: %s <test_name>\n", argv[0]);
    return 1;
  }
  char *test_name = argv[1];
  char sysbuf[1024];

  // malloc()'d so tests under Valgrind can detect
  // out of bounds memory accesses with the tally
  tally_t *tally = calloc(1,sizeof(tally_t));

  IF_TEST("vote_print_1") {
    // Test printing out a simple vote, 4 candidates
    vote_t v = {
      .id=5, .pos=0,
      .candidate_order = {2, 1, 3, 0, NO_CANDIDATE}
    };
    vote_print(&v); printf("\n");
  } // ENDTEST

  IF_TEST("vote_print_2") {
    // Test printing out a simple vote, 5 candidates,
    // pos set to 3
    vote_t v = {
      .id=123, .pos=3,
      .candidate_order = {4, 1, 2, 0, 3, NO_CANDIDATE}
    };
    vote_print(&v); printf("\n");
  } // ENDTEST

  IF_TEST("vote_print_3") {
    // Test printing out a simple vote, 6 candidates,
    // pos set to after candidates so no < > printed
    // as NO_CANDIDATE is indicated
    vote_t v = {
      .id=5026, .pos=6,
      .candidate_order = {1, 4, 2, 5, 3, 0, NO_CANDIDATE}
    };
    vote_print(&v); printf("\n");
  } // ENDTEST

  IF_TEST("vote_next_candidate_1") {
    // Single call to vote_next_candidate() to advance
    // pos field to the next ACTIVE candidate which is
    // in the next position of candidate_order[] field
    vote_t v = {
      .id=8, .pos=0,
      .candidate_order = {1, 4, 2, 3, 0, NO_CANDIDATE}
      //          status: M  A  A  D  A
    };
    char cand_status[] = {
      CAND_ACTIVE,              // 0
      CAND_MINVOTES,            // 1
      CAND_ACTIVE,              // 2
      CAND_DROPPED,             // 3
      CAND_ACTIVE,              // 4
    };
    printf("0 Initial\n");
    vote_print(&v); printf("\n");
    int res = vote_next_candidate(&v, cand_status);
    printf("1 After vote_next_candidate()\n");
    vote_print(&v); printf("\n");
    printf("res: %d\n",res);
  } // ENDTEST

  IF_TEST("vote_next_candidate_2") {
    // Call to vote_next_candidate() should advance
    // pos 2 positions from 0 to 2 as candidate at
    // pos 1 has status DROPPED so is ineligible for
    // the vote to transfer to it.
    vote_t v = {
      .id=73, .pos=0,
      .candidate_order = {1, 4, 2, 3, 0, NO_CANDIDATE}
      //          status: M  D  A  A  A
    };
    char cand_status[] = {
      CAND_ACTIVE,              // 0
      CAND_MINVOTES,            // 1
      CAND_ACTIVE,              // 2
      CAND_ACTIVE,              // 3
      CAND_DROPPED,             // 4
    };
    printf("0 Initial\n");
    vote_print(&v); printf("\n");
    int res = vote_next_candidate(&v, cand_status);
    printf("1 After vote_next_candidate()\n");
    vote_print(&v); printf("\n");
    printf("res: %d\n",res);
  } // ENDTEST

  IF_TEST("vote_next_candidate_3") {
    // Call to vote_next_candidate() should advance
    // several positions forward as the candidates
    // after the intial pos are all DROPPED or
    // MINVOTE with the only ACTIVE candidate
    // towards the end of candidate_order[].
    vote_t v = {
      .id=924, .pos=1,
      .candidate_order = {1, 2, 3, 0, 5, 4, NO_CANDIDATE}
      //          status: D  M  M  D  A  A
    };
    char cand_status[] = {
      CAND_DROPPED,             // 0
      CAND_DROPPED,             // 1
      CAND_MINVOTES,            // 2
      CAND_MINVOTES,            // 3
      CAND_ACTIVE,              // 4
      CAND_ACTIVE,              // 5
    };
    printf("0 Initial\n");
    vote_print(&v); printf("\n");
    int res = vote_next_candidate(&v, cand_status);
    printf("1 After vote_next_candidate()\n");
    vote_print(&v); printf("\n");
    printf("res: %d\n",res);
  } // ENDTEST

  IF_TEST("vote_next_candidate_4") {
    // When calling on a vote with pos already at
    // NO_CANDIDATE in candidate_order[], no
    // changes are made by
    // vote_next_candidate(). Function should
    // return vale NO_CANDIDATE which is defined
    // as integer constant -1.
    vote_t v = {
      .id=4891, .pos=3,
      .candidate_order = {1, 2, 0, NO_CANDIDATE}
      //          status: D  D  M
    };
    char cand_status[] = {
      CAND_DROPPED,             // 0
      CAND_DROPPED,             // 1
      CAND_MINVOTES,            // 2
    };
    printf("0 Initial\n");
    vote_print(&v); printf("\n");
    int res = vote_next_candidate(&v, cand_status);
    printf("1 After vote_next_candidate()\n");
    vote_print(&v); printf("\n");
    printf("res: %d\n",res);
  } // ENDTEST

  IF_TEST("tally_print_table_1s") {
    // Print table results for 4 candidates, all
    // candidates active
    tally_t t = {
      .candidate_count = 4,
      .candidate_names =
      {"Francis","Claire","Heather","Viktor"},
      .candidate_vote_counts =
      {4,         1,       0,        2},
      .candidate_status =
      {CAND_ACTIVE,CAND_ACTIVE,CAND_ACTIVE,CAND_ACTIVE},
    };
    tally_print_table(&t);
  } // ENDTEST

  IF_TEST("tally_print_table_2s") {
    // Print table results for 4 candidates, 2
    // candidates dropped so count and percent are
    // printed as "-"
    tally_t t = {
      .candidate_count = 5,
      .candidate_names =
      {"Rick","Morty","Summer","Jerry","Beth"},
      .candidate_vote_counts =
      {199,     0,      65,     0,      87},
      .candidate_status =
      {CAND_ACTIVE,CAND_DROPPED,CAND_ACTIVE,
       CAND_DROPPED,CAND_ACTIVE},
    };
    tally_print_table(&t);
  } // ENDTEST

  IF_TEST("tally_print_table_3s") {
    // Check special case of printing 100.0% and
    // does print candidate with MINVOTE status
    tally_t t = {
      .candidate_count = 3,
      .candidate_names =
      {"Squanchy","Gearhead","Birdperson"},
      .candidate_vote_counts =
      { 0,          0,        725},
      .candidate_status =
      {CAND_MINVOTES,CAND_DROPPED,CAND_ACTIVE},
    };
    tally_print_table(&t);
  } // ENDTEST

  IF_TEST("tally_print_table_1") {
    // Print table results for 4 candidates, all
    // candidates active
    //              NAME      STATUS      VOTE_COUNT
    tally_add(tally,"Francis",CAND_ACTIVE, 4); // 0
    tally_add(tally,"Claire", CAND_ACTIVE, 1); // 1
    tally_add(tally,"Heather",CAND_ACTIVE, 0); // 2
    tally_add(tally,"Viktor", CAND_ACTIVE, 2); // 3
    tally_print_table(tally);
  } // ENDTEST

  IF_TEST("tally_print_table_2") {
    // Print table results for 4 candidates, 2
    // candidates dropped so count and percent are
    // printed as "-"
    //               NAME      STATUS      VOTE_COUNT
    tally_add(tally, "Rick",   CAND_ACTIVE,  199); // 0
    tally_add(tally, "Morty",  CAND_DROPPED,   0); // 1
    tally_add(tally, "Summer", CAND_MINVOTES, 65); // 2
    tally_add(tally, "Jerry",  CAND_DROPPED,   0); // 3
    tally_add(tally, "Beth",   CAND_ACTIVE,   87); // 4
    tally_print_table(tally);
  } // ENDTEST

  IF_TEST("tally_print_table_3") {
    // Check special case of printing 100.0% and
    // does print candidate with MINVOTE status
    //               NAME          STATUS      VOTE_COUNT
    tally_add(tally, "Squanchy",   CAND_MINVOTES,  0); // 0
    tally_add(tally, "Gearhead",   CAND_DROPPED,   0); // 1
    tally_add(tally, "Birdperson", CAND_ACTIVE,  725); // 2
    tally_print_table(tally);
  } // ENDTEST

  IF_TEST("tally_set_minvote_candidates_1"){
    // Determine among 4 candidates which single
    // candidate has the minimum number of votes
    tally_add(tally,"Francis",CAND_ACTIVE, 8); // 0
    tally_add(tally,"Claire", CAND_ACTIVE, 5); // 1
    tally_add(tally,"Heather",CAND_ACTIVE, 2); // 2 - M
    tally_add(tally,"Viktor", CAND_ACTIVE, 6); // 3
    printf("0 Initial\n");
    tally_print_table(tally);
    tally_set_minvote_candidates(tally);
    printf("1 After tally_set_minvote_candidates(tally)\n");
    tally_print_table(tally);
  } // ENDTEST

  IF_TEST("tally_set_minvote_candidates_2a"){
    // Determine among 5 candidates which three
    // candidates have the minimum number of votes
    tally_add(tally,"Francis",CAND_ACTIVE, 4); // 0 - M
    tally_add(tally,"Claire", CAND_ACTIVE,10); // 1
    tally_add(tally,"Heather",CAND_ACTIVE,12); // 2
    tally_add(tally,"Viktor", CAND_ACTIVE, 4); // 3 - M
    tally_add(tally,"Tusk",   CAND_ACTIVE, 4); // 4 - M
    printf("0 Initial\n");
    tally_print_table(tally);
    tally_set_minvote_candidates(tally);
    printf("1 After tally_set_minvote_candidates(tally)\n");
    tally_print_table(tally);
  } // ENDTEST

  IF_TEST("tally_set_minvote_candidates_2b"){
    // Determine among 6 candidates which two
    // candidates have the Minimum number of votes
    // ignoring the Dropped candidates. This test
    // has a DROPPED candidate with a non-zero vote
    // count to ensure that the candidate_status[]
    // is used rather than 0 vote counts for
    // dropping; it is not generally expected that
    // a Dropped candidate will have non-zero votes
    // in a real election.
    tally_add(tally,"Francis",CAND_ACTIVE,   8); // 0
    tally_add(tally,"Claire", CAND_ACTIVE,  10); // 1
    tally_add(tally,"Heather",CAND_ACTIVE,   3); // 2 - M
    tally_add(tally,"Viktor", CAND_DROPPED,  0); // 3
    tally_add(tally,"Edmond", CAND_DROPPED,  2); // 4 - Tricky
    tally_add(tally,"Doug",   CAND_ACTIVE,   3); // 5 - M
    printf("0 Initial\n");
    tally_print_table(tally);
    tally_set_minvote_candidates(tally);
    printf("1 After tally_set_minvote_candidates(tally)\n");
    tally_print_table(tally);
  } // ENDTEST

  IF_TEST("tally_set_minvote_candidates_log"){
    // Similar to a previous check but sets
    // LOG_LEVEL=LOG_MINVOTES which should print
    // detailed information about the minimum vote
    // determination including the minimum votes
    // and the candidates which have the minimum
    // votes.
    LOG_LEVEL=LOG_MINVOTE;  // enable logging of minvote 
    tally_add(tally,"Francis",CAND_ACTIVE, 4); // 0 - M
    tally_add(tally,"Claire", CAND_ACTIVE,10); // 1
    tally_add(tally,"Heather",CAND_ACTIVE,12); // 2
    tally_add(tally,"Viktor", CAND_ACTIVE, 4); // 3 - M
    tally_add(tally,"Tusk",   CAND_ACTIVE, 4); // 4 - M
    printf("0 Initial\n");
    tally_print_table(tally);
    tally_set_minvote_candidates(tally);
    printf("1 After tally_set_minvote_candidates(tally)\n");
    tally_print_table(tally);
  } // ENDTEST

  IF_TEST("tally_condition_continue"){
    // 2 or more active candidates is a condition
    // TALLY_CONTINUE : the election goes on; check
    // several cases for this.
    int ret;
    tally_reset(tally);
    tally_add(tally,"Francis",CAND_ACTIVE,  8); // 0
    tally_add(tally,"Claire", CAND_DROPPED, 0); // 1
    tally_add(tally,"Heather",CAND_MINVOTES,2); // 2
    tally_add(tally,"Viktor", CAND_ACTIVE,  6); // 3
    tally_add(tally,"Edmond", CAND_MINVOTES,2); // 4
    ret = tally_condition(tally);
    printf("CASE 1 ret: %s (%d)\n",condition2str(ret),ret);
    tally_reset(tally);
    tally_add(tally,"Ryu",    CAND_ACTIVE, 5); // 0
    tally_add(tally,"Ken",    CAND_DROPPED,0); // 1
    tally_add(tally,"Chun-Li",CAND_ACTIVE, 6); // 2
    tally_add(tally,"Juri",   CAND_ACTIVE, 6); // 3
    tally_add(tally,"Marissa",CAND_ACTIVE, 4); // 4
    tally_add(tally,"Zangief",CAND_ACTIVE, 3); // 5
    ret = tally_condition(tally);
    printf("CASE 2 ret: %s (%d)\n",condition2str(ret),ret);
  } // ENDTEST

  IF_TEST("tally_condition_win"){
    // One active candidate is condition
    // TALLY_WINNER : the election ends with a
    // winner; check several cases for this.
    int ret;
    tally_reset(tally);
    tally_add(tally,"Francis",CAND_DROPPED, 0); // 0
    tally_add(tally,"Claire", CAND_ACTIVE,  8); // 1 - W
    tally_add(tally,"Heather",CAND_MINVOTES,2); // 2
    tally_add(tally,"Viktor", CAND_DROPPED, 0); // 3
    tally_add(tally,"Edmond", CAND_MINVOTES,2); // 4
    ret = tally_condition(tally);
    printf("CASE 1 ret: %s (%d)\n",condition2str(ret),ret);
    tally_reset(tally);
    tally_add(tally,"Ryu",    CAND_DROPPED,0); // 0
    tally_add(tally,"Ken",    CAND_DROPPED,0); // 1
    tally_add(tally,"Chun-Li",CAND_DROPPED,0); // 2
    tally_add(tally,"Juri",   CAND_DROPPED,0); // 3
    tally_add(tally,"Marissa",CAND_DROPPED,0); // 4
    tally_add(tally,"Zangief",CAND_ACTIVE, 6); // 5 - W
    ret = tally_condition(tally);
    printf("CASE 2 ret: %s (%d)\n",condition2str(ret),ret);
  } // ENDTEST

  IF_TEST("tally_condition_tie"){
    // 0 ACTIVE candidates and multiple MINVOTE
    // candidates is condition TALLY_TIE : the
    // election ends with multi-way; check several
    // cases for this.
    int ret;
    tally_reset(tally);
    tally_add(tally,"Francis",CAND_MINVOTES, 5); // 0 - T
    tally_add(tally,"Claire", CAND_DROPPED,  0); // 1
    tally_add(tally,"Heather",CAND_DROPPED,  0); // 2
    tally_add(tally,"Viktor", CAND_DROPPED,  0); // 3
    tally_add(tally,"Edmond", CAND_MINVOTES, 5); // 4 - T
    ret = tally_condition(tally);
    printf("CASE 1 ret: %s (%d)\n",condition2str(ret),ret);
    tally_reset(tally);
    tally_add(tally,"Ryu",    CAND_DROPPED,  0); // 0
    tally_add(tally,"Ken",    CAND_MINVOTES,12); // 1 - T
    tally_add(tally,"Chun-Li",CAND_MINVOTES,12); // 2 - T
    tally_add(tally,"Juri",   CAND_MINVOTES,12); // 3 - T
    tally_add(tally,"Marissa",CAND_MINVOTES,12); // 4 - T
    tally_add(tally,"Zangief",CAND_DROPPED,  0); // 5
    ret = tally_condition(tally);
    printf("CASE 2 ret: %s (%d)\n",condition2str(ret),ret);
  } // ENDTEST

  IF_TEST("tally_condition_error"){
    // Conditions that are not a CONTINUE, TIE, or
    // WIN yield a TALLY_ERROR; check several
    // cases for this.
    int ret;
    tally_reset(tally);
    tally_add(tally,"Francis",CAND_DROPPED, 0); // 0
    tally_add(tally,"Claire", CAND_DROPPED, 0); // 1
    tally_add(tally,"Heather",CAND_DROPPED, 0); // 2
    tally_add(tally,"Viktor", CAND_DROPPED, 0); // 3
    tally_add(tally,"Edmond", CAND_DROPPED, 0); // 4
    ret = tally_condition(tally);
    printf("CASE 1 ret: %s (%d)\n",condition2str(ret),ret);
    tally_reset(tally);
    tally_add(tally,"Ryu",    CAND_DROPPED,  0); // 0
    tally_add(tally,"Ken",    CAND_DROPPED,  0); // 1
    tally_add(tally,"Chun-Li",CAND_DROPPED,  0); // 2
    tally_add(tally,"Juri",   CAND_DROPPED,  0); // 3
    tally_add(tally,"Marissa",CAND_MINVOTES, 2); // 4
    tally_add(tally,"Zangief",CAND_DROPPED,  0); // 5
    ret = tally_condition(tally);
    printf("CASE 2 ret: %s (%d)\n",condition2str(ret),ret);
    tally_reset(tally);
    tally_add(tally,"Francis",CAND_DROPPED, 0); // 0
    tally_add(tally,"Claire", CAND_UNKNOWN, 0); // 1
    tally_add(tally,"Heather",CAND_DROPPED, 0); // 2
    ret = tally_condition(tally);
    printf("CASE 3 ret: %s (%d)\n",condition2str(ret),ret);
  } // ENDTEST

  IF_TEST("vote_make_empty"){
    // Check basic functionality of vote creation
    // via heap allocation
    vote_t *vote = vote_make_empty();
    vote->id = 251;
    vote->pos = 2;
    vote->candidate_order[0] = 3;
    vote->candidate_order[1] = 0;
    vote->candidate_order[2] = 4;
    vote->candidate_order[3] = 2;
    vote->candidate_order[4] = 1;
    vote->candidate_order[5] = NO_CANDIDATE;
    printf("vote: ");
    vote_print(vote); 
    printf("\n");
    vote_t *empty = vote_make_empty();
    printf("empty { id: %d, pos: %d, next: %p\n",
           empty->id, empty->pos, empty->next);
    printf("        candidate_order[]: {\n");
    for(int i=0; i<MAX_CANDIDATES; i++){
      printf("           [%d]: %d\n",
             i,empty->candidate_order[i]);
    }
    printf("        }\n}\n");
    free(vote);
    free(empty);
  } // ENDTEST

  IF_TEST("tally_add_vote_print_free_1"){
    // Create a tally with 4 candidates then print
    // it; each candidate has 0 votes. Then add 3
    // votes to it, 1 vote for candidate 0, 2 votes
    // for candidate 2. Print the tally table and
    // votes show placement is correct.
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0, 1 vote
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1, 0 votes
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2, 2 votes
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3, 0 votes
    printf("CASE 1: Tally with 0 votes\n");
    tally_print_votes(t);
    tally_add_vote(t,vote_make( 1,0,0,3,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,2,0,1,3,NO_CANDIDATE)); 
    printf("\nCASE 2: 1 vote for candidate 0, 2 for candidate 2\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: De-allocate the tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_add_vote_print_free_2"){
    // Create a larger tally with 4 candidates and
    // 10 total votes spread across each of
    // them. Print table/votes to show votes added
    // to the correct candidates.
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3
    tally_add_vote(t,vote_make( 1,0,0,3,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,1,0,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 4,0,1,0,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 5,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 6,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 7,0,2,0,1,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 8,0,3,0,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 9,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(10,0,2,0,1,3,NO_CANDIDATE)); 
    tally_print_table(t);
    tally_print_votes(t);
    tally_free(t);
  } // ENDTEST

  IF_TEST("tally_add_vote_print_free_3"){
    // Larger tally with 8 candidates 20 votes
    // added via repeated calls
    // tally_add_vote(). tally_print_votes() called
    // midway and at end; tally free()'d at end.
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    char *cands[] = {"A","B","C","D","E","F","G","H",NULL};
    for(int i=0; cands[i]!=NULL; i++){
      tally_add(t,cands[i],CAND_ACTIVE,0);
    }
    tally_add_vote(t,vote_make( 1,0,3,6,1,0,4,2,5,7,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 2,0,7,3,2,1,0,6,5,4,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 3,0,0,4,6,2,7,1,5,3,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 4,0,4,0,3,6,2,1,7,5,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 5,0,2,5,0,1,4,7,3,6,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 6,0,7,0,6,3,4,5,1,2,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 7,0,0,5,3,4,1,7,2,6,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 8,0,3,4,0,7,6,2,5,1,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 9,0,7,5,2,3,1,0,4,6,NO_CANDIDATE));
    tally_add_vote(t,vote_make(10,0,1,6,5,2,3,4,7,0,NO_CANDIDATE));
    printf("CASE 1: 10 votes added\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_add_vote(t,vote_make(11,0,2,0,3,7,1,5,6,4,NO_CANDIDATE));
    tally_add_vote(t,vote_make(12,0,2,1,4,6,7,0,5,3,NO_CANDIDATE));
    tally_add_vote(t,vote_make(13,0,5,0,4,3,1,6,2,7,NO_CANDIDATE));
    tally_add_vote(t,vote_make(14,0,4,1,3,5,2,6,7,0,NO_CANDIDATE));
    tally_add_vote(t,vote_make(15,0,0,1,3,4,6,5,7,2,NO_CANDIDATE));
    tally_add_vote(t,vote_make(16,0,7,5,2,1,3,6,4,0,NO_CANDIDATE));
    tally_add_vote(t,vote_make(17,0,7,5,1,3,2,0,6,4,NO_CANDIDATE));
    tally_add_vote(t,vote_make(18,0,3,0,2,6,7,4,5,1,NO_CANDIDATE));
    tally_add_vote(t,vote_make(19,0,3,6,7,4,1,0,2,5,NO_CANDIDATE));
    tally_add_vote(t,vote_make(20,0,4,2,3,0,6,1,5,7,NO_CANDIDATE));
    printf("\nCASE 2: 20 votes added\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_transfer_first_vote_1"){
    // Moves a single vote from candidate 2
    // (Heather) to candidate 1 (Claire) with
    // Candidate 1 has 1 vote to which the second
    // is added. Tests proper handling of a single
    // node destination list.
    // LOG_LEVEL=LOG_VOTE_TRANSFERS so that
    // additional information is printed when votes
    // are transferred.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0, 2 votes
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1, 1 vote
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2, vote to 1
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3
    tally_add_vote(t,vote_make( 1,0,0,2,3,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,0,2,3,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,1,2,3,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 4,0,2,1,3,0,NO_CANDIDATE)); 
    printf("CASE 1: before transfer\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,2); // Heather's vote to Claire
    printf("\nCASE 2: after transfer of vote from candidate 2\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_transfer_first_vote_2"){
    // Moves a single vote from candidate 2
    // (Heather) to candidate 1 (Claire) with
    // Candidate 1 having 0 votes (empty list) to
    // begin with. Tests proper handling of a NULL
    // destination list.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2, vote to 1
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3
    tally_add_vote(t,vote_make( 1,0,0,2,3,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,0,2,3,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,2,1,3,0,NO_CANDIDATE)); 
    printf("CASE 1: before transfer\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,2); // Heather's vote to Claire
    printf("\nCASE 2: after transfer of vote from candidate 2\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST


  IF_TEST("tally_transfer_first_vote_3"){
    // Ensure that transfering a vote from a
    // candidate with 0 votes (empty vote list)
    // does not cause problems. This situation is
    // not expected to occur in an actual election
    // but is a requirement of the transfer
    // function for robustness.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2, vote to 1
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3
    tally_add_vote(t,vote_make( 1,0,0,2,3,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,0,2,3,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,2,1,3,0,NO_CANDIDATE)); 
    printf("CASE 1: before transfer\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,2); // Heather's vote to Claire
    printf("\nCASE 2: after transfer of vote from candidate 2\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST


  IF_TEST("tally_transfer_first_vote_4"){
    // Tests several successive transfers from 2
    // different candidates. Transfer logging is
    // enabled.
    LOG_LEVEL=LOG_VOTE_TRANSFERS; // enable transfer logging
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3
    tally_add_vote(t,vote_make( 1,0,0,3,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,1,0,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 4,0,1,0,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 5,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 6,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 7,0,2,0,1,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 8,0,3,0,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 9,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(10,0,2,0,1,3,NO_CANDIDATE)); 
    printf("CASE 1: before transfer\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,3); // Victor's vote to Francis
    printf("\nCASE 2: after transfer from candidate 3\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,1);   // Claire's votes to Francis
    tally_transfer_first_vote(t,1);
    printf("\nCASE 3: after transfer from candidate 1\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 4: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_transfer_first_vote_5"){
    // Like tally_transfer_first_vote_4 but with
    // the LOG_LEVEL=0 so that none of the trnasfer
    // log messsages are printed.
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3
    tally_add_vote(t,vote_make( 1,0,0,3,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,1,0,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 4,0,1,0,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 5,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 6,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 7,0,2,0,1,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 8,0,3,0,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 9,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(10,0,2,0,1,3,NO_CANDIDATE)); 
    printf("CASE 1: before transfer\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,3); // Victor's vote to Francis
    printf("\nCASE 2: after transfer from candidate 3\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,1);   // Claire's votes to Francis
    tally_transfer_first_vote(t,1);
    printf("\nCASE 3: after transfer from candidate 1\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 4: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_transfer_first_vote_6"){
    // Tests proper transfer when votes must "skip"
    // dropped candidates. vote_next_candidate()
    // must return an ACTIVE candidate which will
    // be the transfer target and this code tests
    // that return value is used during transfers.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE,   0); // 0
    tally_add(t,"Claire", CAND_DROPPED,  0); // 1
    tally_add(t,"Heather",CAND_MINVOTES, 0); // 2
    tally_add(t,"Viktor", CAND_MINVOTES, 0); // 3
    tally_add(t,"Edmond", CAND_ACTIVE,   0); // 4
    tally_add_vote(t,vote_make( 1,0,2,3,1,4,0,NO_CANDIDATE)); // 2, skip 3, skip 1, 4
    tally_add_vote(t,vote_make( 2,0,2,3,4,1,0,NO_CANDIDATE)); // 2, skip 3, 4
    tally_add_vote(t,vote_make( 3,0,2,1,0,3,4,NO_CANDIDATE)); // 2, skip 1, 0
    tally_add_vote(t,vote_make( 4,0,0,1,2,3,4,NO_CANDIDATE)); // for 0
    tally_add_vote(t,vote_make( 5,0,0,1,2,3,4,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 6,0,0,1,2,3,4,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 7,0,4,1,2,3,0,NO_CANDIDATE)); // for 4
    tally_add_vote(t,vote_make( 8,0,4,1,2,3,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 9,0,4,1,2,3,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(10,0,3,4,1,2,0,NO_CANDIDATE)); // for 3
    tally_add_vote(t,vote_make(11,1,1,3,2,0,4,NO_CANDIDATE)); // 2nd choice
    tally_add_vote(t,vote_make(12,1,1,3,4,2,0,NO_CANDIDATE)); // 2nd choice
    printf("CASE 1: before transfer\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,2); // 2->1 skip->0
    printf("\nCASE 2: 1st transfer from candidate 2\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,2); // 2->3 skip->4
    printf("\nCASE 3: 2nd transfer from candidate 2\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,2); // 2->3 skip->1 skip->4
    printf("\nCASE 4: 3rd transfer from candidate 2\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,3); // (1)->3->4
    printf("\nCASE 5: 1st transfer from candidate 3\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_transfer_first_vote(t,3); // (1)->3->2 skip->0
    printf("\nCASE 6: 2nd transfer from candidate 3\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 7: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_drop_minvote_candidates_1"){
    // Drop a minvotes candidate with 0 votes. No
    // votes need to be transferred. Logging is
    // enabled to print that the drop.
    LOG_LEVEL=LOG_DROP_MINVOTES;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE,  0); // 0, 1 votes
    tally_add(t,"Claire", CAND_ACTIVE,  0); // 1, 1 votes
    tally_add(t,"Heather",CAND_MINVOTES,0); // 2, 0 votes
    tally_add(t,"Viktor", CAND_ACTIVE,  0); // 3, 1 votes
    tally_add_vote(t,vote_make( 1,0,0,2,3,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,1,2,3,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,3,1,2,0,NO_CANDIDATE)); 
    printf("CASE 1: before drop minvotes\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_drop_minvote_candidates(t); // 2: Heather dropped
    printf("\nCASE 2: after 2: Heather dropped\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_drop_minvote_candidates_2"){
    // Drop a minvotes candidate with 1
    // vote. Logging of vote transfers is enabled
    // so that both the drop and the transfer
    // should be printed.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE,  0); // 0, 2 votes
    tally_add(t,"Claire", CAND_ACTIVE,  0); // 1, 2 votes
    tally_add(t,"Heather",CAND_MINVOTES,0); // 2, 1 votes
    tally_add(t,"Viktor", CAND_ACTIVE,  0); // 3, 2 votes
    tally_add_vote(t,vote_make( 1,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 1,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 2,0,1,2,3,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 3,0,1,2,3,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 4,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make( 5,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make( 6,0,2,1,3,0,NO_CANDIDATE)); // 2
    printf("CASE 1: before drop minvotes\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_drop_minvote_candidates(t); // 2: Heather dropped
    printf("\nCASE 2: after 2: Heather dropped\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST


  IF_TEST("tally_drop_minvote_candidates_3"){
    // Drop a minvotes candidate with several
    // votes. Tests that the dropping iteratively
    // transfers votes from the dropped candidate
    // to others.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE,  0); // 0, 4 votes
    tally_add(t,"Claire", CAND_MINVOTES,0); // 1, 3 votes
    tally_add(t,"Heather",CAND_ACTIVE,  0); // 2, 4 votes
    tally_add(t,"Viktor", CAND_ACTIVE,  0); // 3, 4 votes
    tally_add_vote(t,vote_make( 1,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 2,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 3,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 4,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 5,0,1,2,3,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 6,0,1,0,3,2,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 7,0,1,2,3,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 8,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make( 9,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make(10,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make(11,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make(12,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make(13,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make(14,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make(15,0,3,1,2,0,NO_CANDIDATE)); // 3
    printf("CASE 1: before drop minvotes\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_drop_minvote_candidates(t); // 1: Claire dropped
    printf("\nCASE 2: after 1: Claire dropped\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_drop_minvote_candidates_4"){
    // Drop 2 minvotes candidate each with several
    // votes. Tests that the dropping iteratively
    // transfers votes from the dropped candidate
    // to others and that multiple candidates are
    // handled. Several votes from candidate 3
    // would transfer to candidate 1 but they are
    // both being dropped so those votes should
    // advance further to either candidate 0 or 2.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE,  0); // 0, 4 votes
    tally_add(t,"Claire", CAND_MINVOTES,0); // 1, 3 votes
    tally_add(t,"Heather",CAND_ACTIVE,  0); // 2, 4 votes
    tally_add(t,"Viktor", CAND_MINVOTES,0); // 3, 3 votes
    tally_add_vote(t,vote_make( 1,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 2,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 3,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 4,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 5,0,1,2,3,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 6,0,1,0,3,2,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 7,0,1,2,3,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 8,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make( 9,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make(10,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make(11,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make(12,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make(13,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make(14,0,3,0,2,1,NO_CANDIDATE)); // 3
    printf("CASE 1: before drop minvotes\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_drop_minvote_candidates(t); // 1: Claire+Viktor dropped
    printf("\nCASE 2: after 1: Claire dropped\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_drop_minvote_candidates_5"){
    // Drop 3 minvotes candidate each with several
    // votes. Tests that the dropping iteratively
    // transfers votes from the dropped candidate
    // to others and that multiple candidates are
    // handled. 
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_MINVOTES, 0); // 0: 2 votes
    tally_add(t,"Claire", CAND_ACTIVE,   0); // 1: 3 votes
    tally_add(t,"Heather",CAND_MINVOTES, 0); // 2: 2 votes
    tally_add(t,"Viktor", CAND_MINVOTES, 0); // 3: 2 votes
    tally_add(t,"Edmond", CAND_ACTIVE,   0); // 4: 3 votes
    tally_add_vote(t,vote_make( 1,0,0,1,2,3,4,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 2,0,0,4,1,2,3,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make( 3,0,1,0,2,3,4,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 4,0,1,0,2,3,4,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 5,0,1,0,2,3,4,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make( 6,0,2,1,0,3,4,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make( 7,0,2,1,0,3,4,NO_CANDIDATE)); // 2
    tally_add_vote(t,vote_make( 8,0,3,2,1,0,4,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make( 9,0,3,2,1,0,4,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make(10,0,4,3,2,1,0,NO_CANDIDATE)); // 4
    tally_add_vote(t,vote_make(11,0,4,3,2,1,0,NO_CANDIDATE)); // 4
    tally_add_vote(t,vote_make(12,0,4,3,2,1,0,NO_CANDIDATE)); // 4
    printf("CASE 1: before drop minvotes\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_drop_minvote_candidates(t); // Francis+Heather+Viktor dropped
    printf("\nCASE 2: after 3 candidates dropped\n");
    tally_print_table(t);
    tally_print_votes(t);
    printf("\nCASE 3: freeing tally\n");
    tally_free(t);
    printf("DONE\n");
  } // ENDTEST

  IF_TEST("tally_election_1"){
    // 2 candidates, Round 1 determines the
    // minvotes for one leaving the other as active
    // and the winner.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE,0); // 0, 4 votes
    tally_add(t,"Claire", CAND_ACTIVE,0); // 1, 2 votes
    tally_add_vote(t,vote_make(1,0,0,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make(2,0,0,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make(3,0,0,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make(4,0,0,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make(5,0,1,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make(6,0,1,0,NO_CANDIDATE)); // 1
    tally_election(t);
    tally_free(t);
  } // ENDTEST

  IF_TEST("tally_election_2"){
    // Round 1 drops 1 candidate. Round 2
    // determines winner 1: Claire
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE,0); // 0, 2 votes
    tally_add(t,"Claire", CAND_ACTIVE,0); // 1, 2 votes
    tally_add(t,"Heather",CAND_ACTIVE,0); // 2, 1 votes
    tally_add(t,"Viktor", CAND_ACTIVE,0); // 3, 2 votes
    tally_add_vote(t,vote_make(1,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make(2,0,0,2,3,1,NO_CANDIDATE)); // 0
    tally_add_vote(t,vote_make(3,0,1,2,3,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make(4,0,1,2,3,0,NO_CANDIDATE)); // 1
    tally_add_vote(t,vote_make(5,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make(6,0,3,1,2,0,NO_CANDIDATE)); // 3
    tally_add_vote(t,vote_make(7,0,2,1,3,0,NO_CANDIDATE)); // 2
    tally_election(t);
    tally_free(t);
  } // ENDTEST

  IF_TEST("tally_election_3"){
    // Run the sample election described in the
    // project spec which takes 3 rounds and
    // results in Francis winning. Full logging is
    // enabled.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3
    tally_add_vote(t,vote_make( 1,0,0,3,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,1,0,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 4,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 5,0,1,0,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 6,0,0,2,1,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 7,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 8,0,2,1,0,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 9,0,2,0,1,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(10,0,3,0,2,1,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(11,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(12,0,2,0,1,3,NO_CANDIDATE)); 
    tally_election(t);
    tally_free(t);
  } // ENDTEST

  IF_TEST("tally_election_4"){
    // Round 1: Drop 1 minvote candidate. Round 2:
    // determine that there is a 3-way tie which
    // ends the election.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    tally_add(t,"Francis",CAND_ACTIVE, 0); // 0
    tally_add(t,"Claire", CAND_ACTIVE, 0); // 1
    tally_add(t,"Heather",CAND_ACTIVE, 0); // 2
    tally_add(t,"Viktor", CAND_ACTIVE, 0); // 3
    tally_add_vote(t,vote_make( 1,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 2,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 3,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 4,0,0,1,2,3,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 5,0,3,2,1,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 6,0,3,2,1,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 7,0,3,2,1,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 8,0,3,2,1,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make( 9,0,1,2,3,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(10,0,1,2,3,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(11,0,1,2,3,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(12,0,1,2,3,0,NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(13,0,2,1,3,0 NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(14,0,2,3,1,0 NO_CANDIDATE)); 
    tally_add_vote(t,vote_make(15,0,2,0,1,3,NO_CANDIDATE)); 
    tally_election(t);
    tally_free(t);
  } // ENDTEST

  IF_TEST("tally_election_5"){
    // 8-candidate, 20-vote, multi-round
    // election. 4 Rounds ending in a 4-way tie.
    LOG_LEVEL=LOG_VOTE_TRANSFERS;
    tally_t *t = malloc(sizeof(tally_t)); tally_reset(t);
    char *cands[] = {"A","B","C","D","E","F","G","H",NULL};
    for(int i=0; cands[i]!=NULL; i++){ // 8 candidates
      tally_add(t,cands[i],CAND_ACTIVE,0);
    }
    tally_add_vote(t,vote_make( 1,0,3,6,1,0,4,2,5,7,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 2,0,7,3,2,1,0,6,5,4,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 3,0,0,4,6,2,7,1,5,3,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 4,0,4,0,3,6,2,1,7,5,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 5,0,2,5,0,1,4,7,3,6,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 6,0,7,0,6,3,4,5,1,2,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 7,0,0,5,3,4,1,7,2,6,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 8,0,3,4,0,7,6,2,5,1,NO_CANDIDATE));
    tally_add_vote(t,vote_make( 9,0,7,5,2,3,1,0,4,6,NO_CANDIDATE));
    tally_add_vote(t,vote_make(10,0,1,6,5,2,3,4,7,0,NO_CANDIDATE));
    tally_add_vote(t,vote_make(11,0,2,0,3,7,1,5,6,4,NO_CANDIDATE));
    tally_add_vote(t,vote_make(12,0,2,1,4,6,7,0,5,3,NO_CANDIDATE));
    tally_add_vote(t,vote_make(13,0,5,0,4,3,1,6,2,7,NO_CANDIDATE));
    tally_add_vote(t,vote_make(14,0,4,1,3,5,2,6,7,0,NO_CANDIDATE));
    tally_add_vote(t,vote_make(15,0,0,1,3,4,6,5,7,2,NO_CANDIDATE));
    tally_add_vote(t,vote_make(16,0,7,5,2,1,3,6,4,0,NO_CANDIDATE));
    tally_add_vote(t,vote_make(17,0,7,5,1,3,2,0,6,4,NO_CANDIDATE));
    tally_add_vote(t,vote_make(18,0,3,0,2,6,7,4,5,1,NO_CANDIDATE));
    tally_add_vote(t,vote_make(19,0,3,6,7,4,1,0,2,5,NO_CANDIDATE));
    tally_add_vote(t,vote_make(20,0,4,2,3,0,6,1,5,7,NO_CANDIDATE));
    tally_election(t);
    tally_free(t);
  } // ENDTEST

  
  IF_TEST("tally_from_file_votes-2cand-3votes.txt"){
    // Load a small votes file from the data/ directory and
    // print its contents. Logging of File IO events is
    // enabled which should print detailed information about
    // what is happening during the file load.
    LOG_LEVEL=LOG_FILEIO;
    tally_t *t = tally_from_file("data/votes-2cand-3votes.txt");
    tally_print_table(t);
    tally_print_votes(t);
    tally_free(t);
  } // ENDTEST

  IF_TEST("tally_from_file_votes-3cands.txt"){
    // Load a larger votes file from the data/ directory and
    // print its contents. Then disable logging and re-run
    // to ensure that the no LOG messags are printed.
    tally_t *t;
    printf("CASE 1: Enable file io logging, load file\n");
    LOG_LEVEL=LOG_FILEIO;       // file io logging enabled
    t = tally_from_file("data/votes-3cands.txt");
    printf("\nCASE 2: Load finished, print tally\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_free(t);
    printf("\nCASE 3: Disable logging, reload\n");
    LOG_LEVEL=0;                // disable logging
    t = tally_from_file("data/votes-3cands.txt");
    printf("\nCASE 4: Load finished, print tally\n");
    tally_print_table(t);
    tally_print_votes(t);
    tally_free(t);
  } // ENDTEST

  IF_TEST("tally_from_file_votes-sample.txt"){
    // Load the sample election vote file with 4 candidates
    // and 12 votes. No logging enabled
    LOG_LEVEL=0;                // file io logging disabled
    tally_t *t = tally_from_file("data/votes-sample.txt");
    tally_print_table(t);
    tally_print_votes(t);
    tally_free(t);
  } // ENDTEST

  IF_TEST("tally_from_file_votes-5cands.txt"){
    // Loads a votes file with 5 candidates in
    // it. Candidates 1 and 4 do not have any votes so this
    // test ensures their data is still properly intialized.
    LOG_LEVEL=LOG_FILEIO;       // file io logging enabled
    tally_t *t = tally_from_file("data/votes-5cands.txt");
    tally_print_table(t);
    tally_print_votes(t);
    tally_free(t);
  } // ENDTEST


  IF_TEST("tally_from_file_nofile.txt"){
    // Checks that attempting to load a non-existent file
    // will return NULL by detecting that opening the file
    // fails. An error message should be printed when this
    // occurs.
    printf("CASE 1: opening non-existent file, expecting an error\n");
    tally_t *t = tally_from_file("data/no-such-file.txt");
    if(t == NULL){
      printf("NULL returned correctly on failing to opne a file.\n");
    }
    else{
      printf("Non-NULL returned incorrectly\n");
    }
  } // ENDTEST

  free(tally);

  if(nrun == 0){
    printf("No test named '%s' found\n",test_name);
    return 1;
  }

  return 0;
}
