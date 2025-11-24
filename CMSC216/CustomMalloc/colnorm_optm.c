// optimized version of matrix column normalization
#include "colnorm.h"

////////////////////////////////////////////////////////////////////////////////
// REQUIRED: Paste a copy of your sumdiag_benchmark from an ODD grace
// node below.
//
// -------REPLACE WITH YOUR RUN + TABLE --------
// 
// grace3>> ./colnorm_benchmark 
// ==== Matrix Column Normalization Benchmark Version 1 ====
// ------ Tuned for ODD grace.umd.edu machines -----
// Running with REPEATS: 2 and WARMUP: 1
// Running with 4 sizes (max 6001) and 4 thread_counts (max 4)
//   ROWS   COLS   BASE  T   OPTM SPDUP POINT TOTAL 
//   1111   2223  0.029  1  0.000  0.00  0.00  0.00 
//                       2  0.000  0.00  0.00  0.00 
//                       3  0.000  0.00  0.00  0.00 
//                       4  0.000  0.00  0.00  0.00 
//   2049   4098  0.202  1  0.000  0.00  0.00  0.00 
//                       2  0.000  0.00  0.00  0.00 
//                       3  0.000  0.00  0.00  0.00 
//                       4  0.000  0.00  0.00  0.00 
//   4099   8197  2.566  1  0.000  0.00  0.00  0.00 
//                       2  0.000  0.00  0.00  0.00 
//                       3  0.000  0.00  0.00  0.00 
//                       4  0.000  0.00  0.00  0.00 
//   6001  12003  5.801  1  0.000  0.00  0.00  0.00 
//                       2  0.000  0.00  0.00  0.00 
//                       3  0.000  0.00  0.00  0.00 
//                       4  0.000  0.00  0.00  0.00 
// RAW POINTS: 0.00
// TOTAL POINTS: 0 / 35
// -------REPLACE WITH YOUR RUN + TABLE --------


// You can write several different versions of your optimized function
// in this file and call one of them in the last function.

typedef struct{
  matrix_t mat;
  vector_t avg;
  vector_t std;
  int startCol;
  int endCol;
  pthread_barrier_t *barrier;
  pthread_mutex_t *mutex;
} thread_args_t;  // this is the struct that feeds arguments to the individual threads

void *computeInfo(void *arg){
  thread_args_t *args = (thread_args_t*) arg;  // casts the void arg to the struct that holds args
  matrix_t mat = args->mat;
  vector_t avg = args->avg;
  vector_t std = args->std;
  int start = args->startCol;
  int end = args->endCol;
  pthread_barrier_t *barrier = args->barrier;
  pthread_mutex_t *mutex = args->mutex;

  double *csums = calloc((end-start), sizeof(double));  // tried with malloc but it only works with the default values

  for(int j = start; j < end; j++){   // only through assigned columns
    for(int i = 0; i < mat.rows; i++){  // goes through each row
      csums[j-start] += mget(&mat, i, j);   // sums up the columns
    }
  }

  for(int i = start; i < end; i++){  // repeats for every column assigned to it
    double div = csums[i-start]/mat.rows;
    pthread_mutex_lock(mutex);  // lock the (shared) average variable
    VSET(avg, i, div);
    pthread_mutex_unlock(mutex);   // unlocks for others to access
  }
  
  pthread_barrier_wait(barrier);  // synchronizes the threads

  for(int j = start; j < end; j++){   // only sums in the assigned columns
    double columnAvg = VGET(avg, j);
    double squareDiff = 0.0;
    for(int i = 0; i < mat.rows; i++){
        double diff = MGET(mat, i, j) - columnAvg;   // calculuates the difference
        squareDiff += diff * diff;  // squared difference
    }
    double sqt = sqrt(squareDiff / mat.rows);  // does this outside the lock, for speed
    pthread_mutex_lock(mutex);  // locks
    VSET(std, j, sqt);
    pthread_mutex_unlock(mutex);   // unlocks
  }

  pthread_barrier_wait(barrier);  // synchronizes the threads

  for(int j = start; j < end; j++){  // repeats through assigned columns
    double colAvg = VGET(avg, j);   // gets the average
    double colSTD = VGET(std, j);   /// and the standard deviation
    if (colSTD == 0.0) colSTD = 1.0;   // if the standard deviation makes it not divide by 0
    for (int i = 0; i < mat.rows; i++) {
      double normal = (MGET(mat, i, j) - colAvg) / colSTD;   // given formula
      pthread_mutex_lock(mutex);   // locks
      MSET(mat, i, j, normal);
      pthread_mutex_unlock(mutex);  // unlocks
    }
  }
  free(csums);   // free the calloc'ed array
  return NULL;
}


int cn_verA(matrix_t mat, vector_t avg, vector_t std, int thread_count) {
  pthread_t threads[thread_count];
  thread_args_t threadData[thread_count];
  pthread_barrier_t barrier;
  pthread_mutex_t mutex;   // sets up the structures and arrays

  pthread_barrier_init(&barrier, NULL, thread_count);
  pthread_mutex_init(&mutex, NULL);

  
  int columnsPerThread = mat.cols / thread_count;   // calculates how many columns each thread should handle
  int remaining_cols = mat.cols % thread_count;
  for(int i = 0; i < thread_count; i++){
    threadData[i].mat = mat;   // sets up the structure for each thread
    threadData[i].avg = avg;
    threadData[i].std = std;
    threadData[i].startCol = i * columnsPerThread;
    threadData[i].endCol = threadData[i].startCol + columnsPerThread;

    if (i == thread_count - 1) {
      threadData[i].endCol += remaining_cols;   // if you're on last thread make sure to get all columns
    }
    threadData[i].barrier = &barrier;
    threadData[i].mutex = &mutex;   // makes an array of the mutex and barriers

    if(pthread_create(&threads[i], NULL, computeInfo, &threadData[i]) != 0){

      pthread_barrier_destroy(&barrier);  // couldn't make thread so reset early
      pthread_mutex_destroy(&mutex);

      return -1;
    }

  }

  for (int i = 0; i < thread_count; i++) {   // final synchronization
    pthread_join(threads[i], NULL);   // joins all threads
  }

  pthread_barrier_destroy(&barrier);  // deleted the barrier
  
  pthread_mutex_destroy(&mutex);   // and the mutex
  return 0;
}



int cn_verB(matrix_t mat, vector_t avg, vector_t std, int thread_count) {
  return 0;
}


int colnorm_OPTM(matrix_t mat, vector_t avg, vector_t std, int thread_count){
  // call your preferred version of the function
  return cn_verA(mat, avg, std, thread_count);
}

////////////////////////////////////////////////////////////////////////////////
// REQUIRED: DON'T FORGET TO PASTE YOUR TIMING RESULTS FOR
// sumdiag_benchmark FROM AN ODD GRACE NODE AT THE TOP OF THIS FILE
////////////////////////////////////////////////////////////////////////////////
