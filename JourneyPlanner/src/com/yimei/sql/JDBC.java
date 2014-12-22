package com.yimei.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBC {
	
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	static final String DB_URL = "jdbc:mysql://localhost/";
	
	// Database credentials
	static final String USER = "root";
	static final String PASS = "123456";
	
	// methods
	
	// create an empty database (clear the previous one if exists)
	public static void createDatabase(String database) {
		Connection conn = null;
		Statement stmt = null;
		try{
			// Register JDBC driver
			Class.forName("com.mysql.jdbc.Driver");
			
			// Open a connection
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			
			stmt = conn.createStatement();
			String sql;
			
			// Drop the database if it exists
			System.out.println("Clearing database " + database + "...");
			
			sql = "DROP DATABASE IF EXISTS " + database;
			stmt.executeUpdate(sql);
			System.out.println("Database " + database + " cleared successfully...");
			
			// Create the database if it does not exist
			System.out.println("Creating database " + database + "...");
			
			sql = "CREATE DATABASE " + database;
			stmt.executeUpdate(sql);
			System.out.println("Database " + database + " created successfully...");
			
		}catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(stmt!=null)
					stmt.close();
			}catch(SQLException se2){
			}// nothing we can do
			try{
				if(conn!=null)
					conn.close();
			}catch(SQLException se){
				se.printStackTrace();
			}//end finally try
		}//end try
		System.out.println("Goodbye!");
	}
	
	public static Connection connectDatabase(String database) throws ClassNotFoundException, SQLException {
		Connection conn = null;
		Statement stmt = null;
		// Register JDBC driver
		Class.forName("com.mysql.jdbc.Driver");
		
		// Open a connection
//		System.out.println("Connecting to database...");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		stmt = conn.createStatement();
		String sql;
		
		// Select this database
		sql = "USE " + database;
		stmt.executeUpdate(sql);
//		System.out.println("Database selected...");
		
		return conn;
	}
	
	// get the number of rows in the table
	public static int count(String tableName, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "SELECT COUNT(*) FROM " + tableName;
		ResultSet rs = stmt.executeQuery(sql);
		
		int count = 0;
		while(rs.next()) {
	         //Retrieve by column name
			count  = rs.getInt("COUNT(*)");
		}
		
		rs.close();
		stmt.close();
		
		return count;
	}
	
	// get the maximal value of the integer column of a table
	public static int columnMaxInt(String table, String column, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "SELECT MAX(" + column + ") AS Max FROM " + table;
		ResultSet rs = stmt.executeQuery(sql);
		
		int max = -Integer.MAX_VALUE;
		while(rs.next()) {
	        max = rs.getInt("Max");
		}
		
		rs.close();
		stmt.close();
		
		return max;
	}
	
	
	
//	public static void main(String[] args) {
//		
//		createDatabase("Adelaide");
//	}
}
