| Typescript version | scalajs compile stage   | JS file size | app loading time | app resume from background time |
|--------------------|-------------------------|--------------|------------------|---------------------------------|
| 2.3.4              | -                       | -            | 6 s              | 4 s                             |
|                    | fastOptJS               | 2,9 MB       | 9 s              | 80 s                            |
|                    | fastOptJS without circe | 860 KB       | 7 s              | 13 s                            |
|                    | fullOptJS               | 550 KB       | 7 s              | 25 s                            |
|                    | fullOptJS without circe | 180 KB       | 9 s              | 6 s                             |
| 2.4.2              | -                       | -            | 9 s              | 5 s                             |
|                    | fastOptJS               | 2,9 MB       | 58 s             | 77 s                            |
|                    | fastOptJS without circe | 860 KB       | 7 s              | 13 s                            |
|                    | fullOptJS               | 550 KB       | 7 s              | 25 s                            |
|                    | fullOptJS without circe | 180 KB       | 10 s             | 6 s                             |