#!/bin/bash
echo haha
# Set the path to your Java program
JAVA_PROGRAM="src/fr/uga/pddl4j/examples/asp/ASP.java"

# Set the path to the directory containing the problem files
PROBLEM_DIR="resources/"

# Set the path to the output directory
OUTPUT_DIR="results/"

# Compile your Java program
javac "$JAVA_PROGRAM"

# Loop through each domain folder
for DOMAIN_FOLDER in "$PROBLEM_DIR"/*/; do
    # Get the domain name (i.e., the name of the parent directory)
    DOMAIN_NAME="$(basename "$DOMAIN_FOLDER")"
    echo domain name : $DOMAIN_NAME

    # Loop through each problem file
    for PROBLEM_FILE in "$DOMAIN_FOLDER"*.pddl; do
        # Get the problem name without the directory path or extension
        FILENAME="$(basename "$PROBLEM_FILE")"
        FILENAME="${FILENAME%.*}"

        # Construct the output filename
        OUTPUT_FILE="$OUTPUT_DIR/$DOMAIN_NAME$FILENAME.out"

        # Run your Java program with the domain file and problem file as arguments
        java ASP.java "$PROBLEM_DIR/$DOMAIN_NAME/domain.pddl" "$PROBLEM_FILE" > "$OUTPUT_FILE"
    done
done
