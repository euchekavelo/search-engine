## Search Engine

_List of information about specified sites:_
```
siteConfigs:
-   url: https://www.playback.ru
    name: Плейбек.ру
-   url: https://pcworkshop.ru
    name: ПКВоркШоп.ру
```



***
The <strong>Dashboard</strong> section displays indexing statistics for sites set in the application's configuration file.
For each of the sites, it is possible to view more detailed statistics by clicking on the active blocks corresponding to them.

![](/screenshots/Dashboard.PNG)


***
The <strong>Management</strong> section contains the main controls related to the processes of completely starting indexing, stopping it, as well as indexing a single page.

![](/screenshots/Management.PNG)

***
The <strong>Search</strong> section provides the ability to search for full matches of words or phrases, taking into account the registries of words, both on all sites and on a specific one.

![](/screenshots/Search.PNG)

***
### Technology stack
Java 17, Spring Boot, Spring Data JPA, Thymeleaf, Lombok, MySQL, JSOUP, Apache Lucene Morphology, Docker.

***
### Instructions for running a project locally
1. Install and run Docker locally. Instructions can be found on the [official website](https://docs.docker.com/engine/install/).
2. Download and install Apache Maven. Instructions can be found on the [official website](https://maven.apache.org/index.html).
3. Clone the repository of this project to the directory of interest.
    For example, as one of the options, you can use the https method in Git Bash:
    ```
    git clone https://github.com/euchekavelo/search-engine.git
    ```
4. In the cloned repository go to the root of the project through the command line terminal and enter the command, waiting for it to complete:
    ```
    mvn clean package
    ```
5. Immediately, to form containers and then run them, enter the command, waiting for it to complete:
    ```
    docker-compose up -d
    ```
6. After successfully launching containers, using a web browser, navigate to http://localhost:8080/ .