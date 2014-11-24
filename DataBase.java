import com.sleepycat.db.*;
import java.io.*;
import java.util.*;

/*
 * requires refactoring
*/

public class DataBase{
	private static final int NO_RECORDS = 100000;
	private static final String DATABASE_DIR = "./tmp/user_db";
	private static final String PRIMARY_TABLE = "./tmp/user_db/primary_table_file1";
	private static final String SECONDARY_TABLE = "./tmp/user_db/secondary_table_file2";

		
	private static DataBase db = null;	
	private Database database = null;	
	private SecondaryDatabase secdatabase = null;

	
	private Random random;
	private int duplicateKeys;

	// not sure if all these method calls should be in constructor
	protected DataBase(){
		random = new Random(1000000);
		duplicateKeys = 0;
		if(!createDirectory(DATABASE_DIR)){
			System.err.println("Unable to create file for database");
			System.exit(-1);
		}
		if(!createBase()){
			System.err.println("Database was not created properly");
			System.exit(-1);
		}
		populateTable();
		System.out.println(duplicateKeys + " duplicate keys created (none were inserted don't worry)");
	}

	public static DataBase getInstance(){
		if(db == null){
			db = new DataBase();
		}

		return db;
	}

	public Database getPrimaryDb(){
		return this.database;
	}

	public SecondaryDatabase getSecondaryDb(){
		return this.secdatabase;
	}

	private final boolean createDirectory(String file){
		File dbDirect = new File(file);
	  dbDirect.mkdirs();
		return dbDirect.exists();
	}

	private final boolean createBase(){
		// Create the database object.
		// There is no environment for this simple example.
		DatabaseConfig dbConfig = new DatabaseConfig();

		switch(Pref.getDbType()){
			case 1:
							dbConfig.setType(DatabaseType.BTREE);
							break;
			case 2:
							dbConfig.setType(DatabaseType.HASH);
							break;
			case 3:
							configureIndexFileDb();
							return true;
			default:
							System.out.println("Unrecognized database type.");
		}
		
		dbConfig.setAllowCreate(true);
		try{
			this.database = new Database(PRIMARY_TABLE, null, dbConfig);
		}catch (DatabaseException dbe){
			System.err.println("unable to create database");
			dbe.printStackTrace();
		}catch (FileNotFoundException fnfe){
			System.err.println("can not find file to create Database");
			fnfe.printStackTrace();
		}

		if(this.database == null){
			return false;
		}
		
		System.out.println(PRIMARY_TABLE + " has been created of type: " + dbConfig.getType());
		return true;
	}	

	private final boolean configureIndexFileDb(){
		DatabaseConfig primaryConfig = new DatabaseConfig();
		SecondaryConfig secConfig = new SecondaryConfig();

				
		primaryConfig.setAllowCreate(true);
		primaryConfig.setType(DatabaseType.HASH);
		primaryConfig.setSortedDuplicates(false);
		// duplicate code clean up
		try{
			this.database = new Database(PRIMARY_TABLE, null, primaryConfig);
		}catch (DatabaseException dbe){
			System.err.println("unable to create database");
			dbe.printStackTrace();
		}catch (FileNotFoundException fnfe){
			System.err.println("can not find file to create Database");
			fnfe.printStackTrace();
		}
		
		if(this.database == null){
			return false;
		}

		System.out.println(PRIMARY_TABLE + " has been created of type: " + primaryConfig.getType());
		
			
		
		secConfig.setKeyCreator(new FirstCharKeyCreator());
		secConfig.setAllowCreate(true);
		secConfig.setType(DatabaseType.HASH);
		secConfig.setSortedDuplicates(true);
		secConfig.setAllowPopulate(true);

		try{
			this.secdatabase = new SecondaryDatabase(SECONDARY_TABLE, null, this.database, secConfig);
		}catch(DatabaseException dbe){
			System.err.println("Error while instantiating secondary database: " + dbe.toString());
			this.close();
			System.exit(-1);
		}catch(FileNotFoundException fnfe){
			System.err.println("Secondary database file not found: " + fnfe.toString());
		}
		
		System.out.println(SECONDARY_TABLE + " has been created of type: " + secConfig.getType());
		return true;
	}
	 
	private void populateTable() {
		int count = 0;
		while(count < NO_RECORDS){
			count += addEntry();
		}
		System.out.println(NO_RECORDS + " records inserted into" + PRIMARY_TABLE);
	}
	
	private int addEntry(){
		int range;
    DatabaseEntry kdbt, ddbt;
		String s;

		range = 64 + random.nextInt( 64 );
		s = "";
		for ( int j = 0; j < range; j++ ) 
			s+=(new Character((char)(97+random.nextInt(26)))).toString();
		
		kdbt = new DatabaseEntry(s.getBytes());
		kdbt.setSize(s.length()); 

		range = 64 + random.nextInt( 64 );
		s = "";
		for ( int j = 0; j < range; j++ ) 
			s+=(new Character((char)(97+random.nextInt(26)))).toString();
		              
		ddbt = new DatabaseEntry(s.getBytes());
		ddbt.setSize(s.length()); 
		
		OperationStatus result = null;

		try{
			result = this.database.exists(null, kdbt);
		}catch(DatabaseException dbe){
			System.err.println("Unable to check if key exists");
			dbe.printStackTrace();
		}
		if(!result.toString().equals(OperationStatus.NOTFOUND)){
			try{
				this.database.put(null, kdbt, ddbt);
			}catch(DatabaseException dbe){
				System.err.println("Unable to put key/data pair in database");
				dbe.printStackTrace();
			}
			return 1;
		}
		duplicateKeys++;
		return 0;
	}


	public void close(){
		try{
			if(this.secdatabase != null){
				this.secdatabase.close();
			}
			this.database.close();
			this.database.remove(PRIMARY_TABLE,null,null);
		}catch(DatabaseException dbe){
			System.err.println("unable to close database");
			dbe.printStackTrace();
		}catch (FileNotFoundException fnfe){
			System.err.println("can not find file to remove Database");
			fnfe.printStackTrace();
		}
	}

/*
 * secondary keys are the first char in the primary key string 
*/
	private class FirstCharKeyCreator implements SecondaryKeyCreator {
			public boolean createSecondaryKey(SecondaryDatabase secondary,
                                      DatabaseEntry key,
                                      DatabaseEntry data,
                                      DatabaseEntry result)
            throws DatabaseException {
        byte[] firstByte = new byte[1];
        firstByte[0] = data.getData()[0];
        result.setData(firstByte);
        return true;
    }
	}
}