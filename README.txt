This is a simple backend that supports LODmilla-frontend with
- special graph queries
- saving and loading graph screenshots
- accessing linked data without a SPARQL endpoint

How to set up lodmilla-backend?

1. Set up a Mysql server. Create a database, and run graphs.sql in that database.

2. Under src/java create and fill lodmillabackend.properties based on the sample file.

3. Set up Tomcat.

4. Build the war and deploy it under Tomcat. If you use the pre-built war file, you need to create the default user and password for the SQL database given in backend properties. Or you can unpack the war, edit the properties file and pack it again.

5. In lodmilla frontend edit profile.class.js and change this.serverProxyUrl to you newly created backend.

The code is not very clean. Volunteers are welcome to improve it.
No liability for any loss or damage suffered as a result of the use of this code.
Please refer/cite to the authors when re-using this code:

http://eprints.sztaki.hu/8054/
Micsik, András and Turbucz, Sándor and Tóth, Zoltán (2015) Exploring publication metadata graphs with the LODmilla browser and editor.
International Journal on Digital Libraries, 16 (x1). pp. 15-24. DOI:10.1007/s00799-014-0130-2

http://eprints.sztaki.hu/8012/
Micsik, András and Turbucz, Sándor and Györök, Attila (2014) LODmilla: a Linked Data Browser for All.
In: Posters&Demos@SEMANTiCS 2014, 2014.09.04-2014.09.05, Leipzig, Germany.
