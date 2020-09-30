#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters, you need to pass two parameters"
    exit 1
fi

unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    *)          machine=UNKNOWN
esac

if [ "$machine" = 'UNKNOWN' ]; then
  echo "Unknown os... aborting"
  exit 2
fi

echo "Running on $machine.."

OLD_VERSION=$1
NEW_VERSION=$2
FILES_TO_CHANGE=$(git grep -l "$OLD_VERSION" | grep -v ".*\.ai") # there is an ai file that always matches...

if [ -z "$FILES_TO_CHANGE" ]; then
  echo "No files to change..."
  exit 0
fi

echo "Bumping from version $OLD_VERSION to version $NEW_VERSION"
echo "Editing the following files:"
echo ""
echo "$FILES_TO_CHANGE"
echo "----------------------------"

while IFS= read -r line; do
    case "${machine}" in
      Linux*)     sed -i "s/${OLD_VERSION}/${NEW_VERSION}/g" $line;;
      Mac*)       sed -i '' -e "s/${OLD_VERSION}/${NEW_VERSION}/g" $line;;
    esac
    git add $line
done <<< "$FILES_TO_CHANGE"


echo "Press enter to commit:"
read

git commit -e -m "Bump version to $NEW_VERSION"
