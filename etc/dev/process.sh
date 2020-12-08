#!/bin/bash
#
# Bash script to facilitate the testing of the validator as a command-line tool for a given domain.
#
# To use this script:
# 1. Build the project so that the JAR module's JAR is created.
# 2. Make a copy of this script.
# 3. Adapt in the copy the script's configuration variables (see script configuration block below) to match your environment.
# 4. Execute this script from a bash shell. The script also requires the "zip" and "unzip" tools to be defined.
#
# Script configuration - START
#
# The path to the default JAR file for the command line tool (as produced given the maven build).
JAR_PATH="/mnt/d/git/itb/shacl-validator/shaclvalidator-jar/target/validator.jar"
# A temporary folder to be used for processing.
TMP_FOLDER="/mnt/d/_dev/shacl/offline/tmp"
# The folder in which the resulting JAR file will be placed.
PROCESSED_FOLDER="/mnt/d/_dev/shacl/offline/processed"
# The resource folder used to configure the validator (as would be provided to the validator fot the resource root.
RESOURCE_FOLDER="/mnt/d/git/itb/docker/validator-shacl-any"
# The name of the domain within the resource root.
DOMAIN_NAME="resources"
#
# Script configuration - END
#
# No changes should be made in the commands that follow.
echo "Processing $RESOURCE_FOLDER"
rm -rf $TMP_FOLDER
mkdir -p $TMP_FOLDER
rm -rf $PROCESSED_FOLDER
mkdir -p $PROCESSED_FOLDER
cp $JAR_PATH $TMP_FOLDER
(cd $RESOURCE_FOLDER; zip -r $TMP_FOLDER/resources.zip ./*)
unzip $TMP_FOLDER/resources.zip -d $TMP_FOLDER/resources
cp ./validator.jar $TMP_FOLDER
unzip $TMP_FOLDER/validator.jar -d $TMP_FOLDER/validator
rm -rf $TMP_FOLDER/validator.jar
rm -rf $TMP_FOLDER/validator/BOOT-INF/classes/validator-resources.jar
(cd $TMP_FOLDER/resources/; zip -r -0 $TMP_FOLDER/validator/BOOT-INF/classes/validator-resources.jar $DOMAIN_NAME)
(cd $TMP_FOLDER/validator/; zip -r -0 $TMP_FOLDER/validator.jar .)
cp $TMP_FOLDER/validator.jar $PROCESSED_FOLDER/validator.jar