# ---- makefile flags ---- #
GRANITE_MAKEFILE_FANCY ?= 0
GRANITE_MAKEFILE_DEBUG ?= 0

# ---- ✨ fancy colors ✨ ---- #
ifeq ($(GRANITE_MAKEFILE_FANCY), 1)
# use tput for higlighting
highlight = $(shell tput setaf 3 && tput bold)$(1)$(shell tput setaf 0 && tput sgr0)
highlight_mild = $(shell tput setaf 3)$(1)$(shell tput setaf 0)
endif

# mock helpers to avoid issues on unsupported environments
highlight ?= $(1)
highlight_mild ?= $(1)

# use (possibly mocked) helpers for styling
heading = $(info $(call highlight,$(1)))
note = $(info $(call highlight_mild,$(1)))

# ---- build gradle command line ---- #
gradle := ./gradlew

# enable stacktraces for debugging builds
ifeq ($(GRANITE_MAKEFILE_DEBUG), 1)
	gradle += --stacktrace
endif

# ---- project-specific options ---- #
option = -Pgranite.$(1)=$(2)
enable_option = -Pgranite.$(1)=true
disable_option = -Pgranite.$(1)=false

# used to include only certain projects in the build
# see settings.gradle.kts for more details
prepass := GRANITE_BUILD_PREPASS=true

# build feature flags
do_publish := $(call enable_option,release)
do_sign := $(call enable_option,sign)

# ---- targets ---- #
help:
	$(call heading,Welcome to the Granite Makefile!)
	$(info )
	$(call heading,Useful targets:)
	
	$(info 	-$(call highlight_mild,prepare): setup the project for development and run the test suite)
	$(info 	-$(call highlight_mild,prepare-plugins): publish the Kotlin and Gradle plugins to MavenLocal (required to build samples))
	$(info 	-$(call highlight_mild,publish-local): publish all publications to MavenLocal)
	$(info 	-$(call highlight_mild,test): run all test suites)
	$(info 	-$(call highlight_mild,samples): run all code samples)
	$(info 	-$(call highlight_mild,reset): clean build and remove local publications)
	$(info 	-$(call highlight_mild,clean): remove Gradle build files)
	$(info 	-$(call highlight_mild,clean-local-publications): remove publications from the MavenLocal repository)
	$(info )
	
	$(call note,--see README.md for more details--)

prepare: prepare-plugins test
	$(call heading,All set! The build is now ready for development)

prepare-plugins:
	$(call heading,Publishing Granite compiler and Gradle plugins to MavenLocal...)
	$(prepass) $(gradle) \
	:packages:granite-plugin-kotlin:publishToMavenLocal \
	:packages:granite-plugin-gradle:publishToMavenLocal \
	$(do_publish)

publish-local:
	$(call heading,Publishing to MavenLocal...)
	$(gradle) publishToMavenLocal $(do_publish)

test:
	$(call heading,Running test suite...)
	$(gradle) test
	
samples:
	$(call heading,Running code samples...)
	$(gradle) :samples:run
	
reset: clean clean-local-publications
	$(call heading,Cleaned build files and local publications)
	
clean:
	$(call heading,Cleaning build)
	$(gradle) clean
	
clean-local-publications:
	$(call heading,Cleaning publications from MavenLocal)
	rm -fr ~/.m2/repository/io/github/darvld/granite
