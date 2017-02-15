How to set up lodmilla-backend?

1. Set up a Mysql server. Create a database, and run graphs.sql in that database.

2. Under src/java create and fill lodmillabackend.properties based on the sample file.

3. Set up Tomcat.

4. Build the war and deploy it under Tomcat.

5. In lodmilla frontend edit profile.class.js and change this.serverProxyUrl to you newly created backend.

