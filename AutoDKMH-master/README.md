# AutoDKMH
*Command-line tool for automatically registering courses of VNU*

## Advantages
- Totally automatic tool
- Won't stop until successful registering or being killed by you
- Command-line based, you can run from a server which has strong network connection

## Setup
Config account and course codes in ```src/config.properties```
  - ```usr``` - Student code
  - ```passwd``` - Password
  - ```course_codes``` - List of course codes, separated by dot characters (```.```)
  - ```sleep_time``` - Sleep time between two adjacent executings
  
## Run
Run with maven plugin

```mvn install exec:java```
