# Dewbe
** **
 WebService API and
 Network + DB.
 

Youtube API is one of examples, this architecture can be used in any web API.

**Points:**

1. Pagination-- PagedList;
2. Room;
3. Retrofit2;
4. LiveData;
5. Repository;
6. ViewModel;
7. Youtube DATA API;
8. Youtube Android Player API;

The app uses ViewModel to abstract the data from UI and Repository as single source of truth for data. Repository first fetch the data from database if exist than display data to the user and if database is out of data, Repository fetches data from the webservice and update the result in database and reflect the changes to UI from database.

![](https://github.com/ed828a/Dewbe/blob/develop/architect_Net_DB.png)
