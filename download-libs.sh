#!/bin/bash

# Create lib directory if it doesn't exist
mkdir -p lib

# Define library versions
OPTAPLANNER_VERSION="8.13.0.Final"
LOGBACK_VERSION="1.2.11"
COMMONS_CSV_VERSION="1.9.0"
MICROMETER_VERSION="1.9.3"

# Define Maven repository URL
MAVEN_REPO="https://repo1.maven.org/maven2"

# Clean existing files
echo "Cleaning existing libraries..."
rm -f lib/*.jar

# Download OptaPlanner Core and its dependencies
echo "Downloading OptaPlanner Core and dependencies..."
curl -L -f -o lib/optaplanner-core-${OPTAPLANNER_VERSION}.jar ${MAVEN_REPO}/org/optaplanner/optaplanner-core/${OPTAPLANNER_VERSION}/optaplanner-core-${OPTAPLANNER_VERSION}.jar
curl -L -f -o lib/kie-api-7.53.0.Final.jar ${MAVEN_REPO}/org/kie/kie-api/7.53.0.Final/kie-api-7.53.0.Final.jar
curl -L -f -o lib/kie-internal-7.53.0.Final.jar ${MAVEN_REPO}/org/kie/kie-internal/7.53.0.Final/kie-internal-7.53.0.Final.jar
curl -L -f -o lib/drools-core-7.53.0.Final.jar ${MAVEN_REPO}/org/drools/drools-core/7.53.0.Final/drools-core-7.53.0.Final.jar
curl -L -f -o lib/drools-compiler-7.53.0.Final.jar ${MAVEN_REPO}/org/drools/drools-compiler/7.53.0.Final/drools-compiler-7.53.0.Final.jar

# Download Micrometer
echo "Downloading Micrometer..."
curl -L -f -o lib/micrometer-core-${MICROMETER_VERSION}.jar ${MAVEN_REPO}/io/micrometer/micrometer-core/${MICROMETER_VERSION}/micrometer-core-${MICROMETER_VERSION}.jar
curl -L -f -o lib/HdrHistogram-2.1.12.jar ${MAVEN_REPO}/org/hdrhistogram/HdrHistogram/2.1.12/HdrHistogram-2.1.12.jar
curl -L -f -o lib/LatencyUtils-2.0.3.jar ${MAVEN_REPO}/org/latencyutils/LatencyUtils/2.0.3/LatencyUtils-2.0.3.jar

# Download Logback
echo "Downloading Logback..."
curl -L -f -o lib/logback-classic-${LOGBACK_VERSION}.jar ${MAVEN_REPO}/ch/qos/logback/logback-classic/${LOGBACK_VERSION}/logback-classic-${LOGBACK_VERSION}.jar
curl -L -f -o lib/logback-core-${LOGBACK_VERSION}.jar ${MAVEN_REPO}/ch/qos/logback/logback-core/${LOGBACK_VERSION}/logback-core-${LOGBACK_VERSION}.jar
curl -L -f -o lib/slf4j-api-1.7.32.jar ${MAVEN_REPO}/org/slf4j/slf4j-api/1.7.32/slf4j-api-1.7.32.jar

# Download Commons CSV
echo "Downloading Commons CSV..."
curl -L -f -o lib/commons-csv-${COMMONS_CSV_VERSION}.jar ${MAVEN_REPO}/org/apache/commons/commons-csv/${COMMONS_CSV_VERSION}/commons-csv-${COMMONS_CSV_VERSION}.jar

# Download additional dependencies
echo "Downloading additional dependencies..."
curl -L -f -o lib/commons-math3-3.6.1.jar ${MAVEN_REPO}/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar
curl -L -f -o lib/commons-lang3-3.12.0.jar ${MAVEN_REPO}/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar

# Verify downloads
echo "Verifying downloads..."
for jar in lib/*.jar; do
    if [ -s "$jar" ]; then
        echo "✓ $(basename $jar)"
    else
        echo "✗ $(basename $jar) - Download failed or file is empty"
        exit 1
    fi
done

echo "All libraries downloaded to the lib directory." 