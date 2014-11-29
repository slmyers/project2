import com.sleepycat.db.*;
import java.io.*;
import java.util.*;

/*
 * requires refactoring
 * much cleaning required
*/

public class DataBase{
	private static final int NO_RECORDS = 100000;
	private static final int NO_RECORDS_TEST = 11;
	public static final String DATABASE_DIR = "/tmp/slmyers_db";
	public static final String PRIMARY_TABLE = "/tmp/slmyers_db/primary_table_file1";
	public static final String PRIMARY_TABLE2 = "/tmp/slmyers_db/primary_table_file2";
	

	
	private static DataBase db = null;	
	private Database database = null;	
	private Database database_2 = null;
	protected DataBase(){
	}
	
	public static DataBase getInstance(){
		if(db == null){
			db = new DataBase();
		}
		return db;
	}

	public void initDataBase(){
		if(!createDirectory(DATABASE_DIR)){
			System.err.println("Unable to create file	 for database");
			System.exit(-1);
		}
		if(!createBase()){
			System.err.println("Database was not created properly");
			System.exit(-1);
		}
	}

	public Database getPrimaryDb(){
		return this.database;
	}

	public Database getPrimaryDb_2(){
		return this.database_2;
	}

	private final boolean createDirectory(String file){
		File dbDirect = new File(file);
	  dbDirect.mkdirs();
		return dbDirect.exists();
	}

	private final boolean createBase(){
		DatabaseConfig dbConfig = new DatabaseConfig();
		DatabaseConfig dbConfig_2 = new DatabaseConfig();
		int count = 0;
		int count2 = 0;
		
		switch(Pref.getDbType()){
			case 1:
							dbConfig.setType(DatabaseType.BTREE);
							dbConfig_2.setType(DatabaseType.HASH);
							break;
			case 2:
							dbConfig.setType(DatabaseType.HASH);
							dbConfig_2.setType(DatabaseType.HASH);
							break;
			case 3:
							dbConfig.setType(DatabaseType.BTREE);
							dbConfig_2.setType(DatabaseType.HASH);
							break;
			default:
							System.out.println("Unrecognized database type.");
		}
			
		if(dbConfig_2 != null){
			dbConfig_2.setAllowCreate(true);
		}

		dbConfig.setAllowCreate(true);
		try{
			this.database = new Database(PRIMARY_TABLE, null, dbConfig);
			this.database_2 = new Database(PRIMARY_TABLE2, null, dbConfig_2);
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
		System.out.println(PRIMARY_TABLE2 + " has been created of type: " + dbConfig_2.getType());
		
		if(Interval.testMode){
			count = populateTable(this.database, NO_RECORDS_TEST);
			count2 = populateTable(this.database_2, NO_RECORDS_TEST);
		}
		else if(Interval.testDupMode){
			try{
				count2 = populateDupTestTable(this.database_2);
				count = populateDupTestTable(this.database);
			}catch(DatabaseException dbe){
				dbe.printStackTrace();
			}
		}
		else{
			count = populateTable(this.database, NO_RECORDS);
			count2 = populateTable(this.database_2, NO_RECORDS);
		}
		System.out.println(PRIMARY_TABLE + " has been inserted with: " + count + " records");
		System.out.println(PRIMARY_TABLE2 + " has been inserted with: " + count2 + " records");
		return true;
	}	
	
	public int populateDupTestTable(Database my_table) throws DatabaseException{
		DatabaseEntry kdbt, ddbt;
		
		int count = 0;
		String[] dupKeys = Interval.DUP_TEST_KEYS;
		String[] dupData = Interval.DUP_TEST_DATA;
		try {
			for(int i = 0; i < 5; i++){
				kdbt = new DatabaseEntry(dupKeys[i].getBytes());
				kdbt.setSize(dupKeys[i].length());
				kdbt.setReuseBuffer(false);
				ddbt = new DatabaseEntry(dupData[i].getBytes());
				ddbt.setSize(dupData[i].length());
				ddbt.setReuseBuffer(false);
				OperationStatus result;
				result = my_table.exists(null, kdbt);
				if (!result.toString().equals("OperationStatus.NOTFOUND"))
					throw new RuntimeException("Key is already in the database!");

				/* to insert the key/data pair into the database */
		    	if(my_table.putNoOverwrite(null, kdbt, ddbt) != OperationStatus.SUCCESS){
						throw new RuntimeException("can not input test dup data!");
					}
				count++;
			} 
		}catch (DatabaseException dbe) {
			System.err.println("Populate the table: "+dbe.toString());
			this.close();
		  System.exit(1);
		}
		return count;
	}

	public int populateTable(Database my_table, int nrecs ) {
		int range;
		DatabaseEntry kdbt, ddbt;
		int count = 0;
		String s;
		ArrayList<String> testKeys = null;
		ArrayList<String> testData = null;
		if(Interval.testMode){
			testKeys = new ArrayList<String>();
			testData = new ArrayList<String>();
		}
		/*  
		 *  generate a random string with the length between 64 and 127,
		 *  inclusive.
		 *
		 *  Seed the random number once and once only.
		 */
		Random random = new Random(1000000);

		try {

    		for (int i = 0; i < nrecs; i++) {
				/* to generate a key string */
				range = 64 + random.nextInt( 64 );
				s = "";
				for ( int j = 0; j < range; j++ ) 
					s+=(new Character((char)(97+random.nextInt(26)))).toString();
				if(Interval.testMode){
					testKeys.add(s);
				}	
		
				/* to create a DBT for key */
				kdbt = new DatabaseEntry(s.getBytes());
				kdbt.setSize(s.length()); 
				 	

				/* to generate a data string */
				range = 64 + random.nextInt( 64 );
				s = "";
				for ( int j = 0; j < range; j++ ) 
					s+=(new Character((char)(97+random.nextInt(26)))).toString();
				if(Interval.testMode){
					testData.add(s);
				}	
				if(s.equals("djjtchkroyzbyzqycjbjfvkxuwuywywkcvqltyagjavmhpewjuhfqsaawwzwvusrobrzmkbstekgkbawzkl")){
					System.out.println("data under question created");
				}
				/* to create a DBT for data */
				ddbt = new DatabaseEntry(s.getBytes());
				ddbt.setSize(s.length()); 

				//TODO change so program recovers instead of exiting
				OperationStatus result;
				result = my_table.exists(null, kdbt);
				if (!result.toString().equals("OperationStatus.NOTFOUND"))
					throw new RuntimeException("Key is already in the database!");

					/* to insert the key/data pair into the database */
				my_table.putNoOverwrite(null, kdbt, ddbt);
				count++;
			}
		}
		catch (DatabaseException dbe) {
			System.err.println("Populate the table: "+dbe.toString());
		  	System.exit(1);
		}
		if(Interval.testMode){
			//gross code
			String[] keys = Interval.TEST_KEYS_IN_ORDER;
			String[] data = Interval.TEST_DATA;
			if(keys.length != data.length){
				throw new RuntimeException("unequal test keys and test data");
			}			
		
			for(int i = 0; i < keys.length; i++){
				if(!testKeys.contains(keys[i])){
					throw new RuntimeException("test key was not created!");
				}
				if(!testData.contains(data[i])){
					throw new RuntimeException("test data was not created!");
				}
			}
		}
		return count;
	}

	public void close(){
		if(this.database != null){
			try{
				this.database.close();
				this.database.remove(PRIMARY_TABLE,null,null);
				System.out.println("database is closed and removed");
			}catch(DatabaseException dbe){
				System.err.println("unable to close database");
				dbe.printStackTrace();
			}catch (FileNotFoundException fnfe){
				System.err.println("can not find file to remove Database");
				fnfe.printStackTrace();
			}
			database = null;
			db = null;
		}
		if(this.database_2 != null){
			try{
				this.database_2.close();
				this.database_2.remove(PRIMARY_TABLE2,null,null);
				System.out.println("database is closed and removed");
			}catch(DatabaseException dbe){
				System.err.println("unable to close database");
				dbe.printStackTrace();
			}catch (FileNotFoundException fnfe){
				System.err.println("can not find file to remove Database");
				fnfe.printStackTrace();
			}
			database = null;
			db = null;
		}	
	}
}
