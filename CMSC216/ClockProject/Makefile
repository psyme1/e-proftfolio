# CMSC216 Project 3 Makefile
AN = p3
CLASS = 216

# -Wno-comment: disable warnings for multi-line comments, present in
# some tests

# -fpic -pie : defaults on most linux systems but NOT GRACE; include
# these so that students using incorrect syntax to access global
# variables will get errors

CFLAGS = -Wall -Wno-comment -Werror -g -Og -fpic -pie -Wno-unused-result
CC     = gcc $(CFLAGS)
SHELL  = /bin/bash
CWD    = $(shell pwd | sed 's/.*\///g')

PROGRAMS = \
	clock_main \
	test_clock_update \

export PARALLEL?=True		#enable parallel testing if not overridden

all : $(PROGRAMS) puzzlebin

clean :
	rm -f $(PROGRAMS) *.o core vgcore.*

help :
	@echo 'Typical usage is:'
	@echo '  > make                          # build all programs'
	@echo '  > make clean                    # remove all compiled items'
	@echo '  > make zip                      # create a zip file for submission'
	@echo '  > make prob1                    # built targets associated with problem 1'
	@echo '  > make test                     # run all tests'
	@echo '  > make test-prob2               # run test for problem 2'
	@echo '  > make test-prob2 testnum=5     # run problem 2 test #5 only'
	@echo '  > make update                   # download and install any updates to project files'

############################################################
# 'make zip' to create complete.zip for submission
ZIPNAME = $(AN)-complete.zip
zip : clean clean-tests
	rm -f $(ZIPNAME)
	cd .. && zip "$(CWD)/$(ZIPNAME)" -r "$(CWD)"
	@echo Zip created in $(ZIPNAME)
	@if (( $$(stat -c '%s' $(ZIPNAME)) > 10*(2**20) )); then echo "WARNING: $(ZIPNAME) seems REALLY big, check there are no abnormally large test files"; du -h $(ZIPNAME); fi
	@if (( $$(unzip -t $(ZIPNAME) | wc -l) > 256 )); then echo "WARNING: $(ZIPNAME) has 256 or more files in it which may cause submission problems"; fi

############################################################
# `make update` to get project updates
update :
	curl -s https://www.cs.umd.edu/~profk/216/$(AN)-update.sh | /bin/bash 

################################################################################
# clock problem
prob1 : clock_main test_clock_update

# build .o files from corresponding .c files
%.o : %.c clock.h
	$(CC) -c $<

# build assembly object via gcc + debug flags
clock_update_asm.o : clock_update_asm.s clock.h
	$(CC) -c $<

clock_main : clock_main.o clock_sim.o clock_update_asm.o 
	$(CC) -o $@ $^

# clock_update functions testing program
test_clock_update : test_clock_update.o clock_sim.o clock_update_asm.o test_clock_update_asm.s
	$(CC) -o $@ $^

test-prob1 : prob1 test_clock_update test-setup
	./testy -o md test_clock_update.org $(testnum)

################################################################################
# binary debugging problem
test-prob2: puzzlebin test-setup
	./puzzlebin input.txt

.PHONY: puzzlebin		# file already exists, just need to change permissions
puzzlebin :
	chmod u+x puzzlebin

################################################################################
# Testing Targets
test-setup :
	@chmod u+rx testy puzzlebin

test: test-prob1 test-prob2

clean-tests : 
	rm -rf test-results/ test_thermo_update 

