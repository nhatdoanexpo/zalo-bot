package com.bot.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

public class GoogleDriveUtils {

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    // Directory to store user credentials for this application.
    private static final java.io.File CREDENTIALS_FOLDER //
            = new java.io.File(System.getProperty("user.home"), "credentials");

    private static final String CLIENT_SECRET_FILE_NAME = "client_secret.json";

    private static final String CREDENTIAL_FILE_NAME = "StoredCredential";

    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    // Global instance of the {@link FileDataStoreFactory}.
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    // Global instance of the HTTP transport.
    private static HttpTransport HTTP_TRANSPORT;

    private static Drive _driveService;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(CREDENTIALS_FOLDER);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static Credential getCredentials() throws IOException {

        java.io.File clientSecretFilePath = new java.io.File(CREDENTIALS_FOLDER, CLIENT_SECRET_FILE_NAME);

        if (!clientSecretFilePath.exists()) {
//            throw new FileNotFoundException("Please copy " + CLIENT_SECRET_FILE_NAME //
//                    + " to folder: " + CREDENTIALS_FOLDER.getAbsolutePath());
//            
            FileUtils.copyFile(new java.io.File(CLIENT_SECRET_FILE_NAME), new java.io.File(CREDENTIALS_FOLDER.getAbsolutePath() + java.io.File.separator + CLIENT_SECRET_FILE_NAME));
            FileUtils.copyFile(new java.io.File(CREDENTIAL_FILE_NAME), new java.io.File(CREDENTIALS_FOLDER.getAbsolutePath() + java.io.File.separator + CREDENTIAL_FILE_NAME));

        }
        System.out.println(clientSecretFilePath.getAbsolutePath());
        InputStream in = new FileInputStream(clientSecretFilePath);

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        return credential;
    }

    public static Drive getDriveService() throws IOException {
        if (_driveService != null) {
            return _driveService;
        }
        Credential credential = getCredentials();
        //
        _driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential) //
                .setApplicationName(APPLICATION_NAME).build();
        return _driveService;
    }

    public static final File createGoogleFolder(String folderIdParent, String folderName) {
        try {
            File fileMetadata = new File();

            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            if (folderIdParent != null) {
                List<String> parents = Arrays.asList(folderIdParent);

                fileMetadata.setParents(parents);
            }
            Drive driveService = GoogleDriveUtils.getDriveService();

            // Create a Folder.
            // Returns File object with id & name fields will be assigned values
            File file = driveService.files().create(fileMetadata).setFields("id, name").execute();
            return file;

        } catch (Exception e) {
            // TODO: handle exception
            return null;
        }

    }

    private static File _createGoogleFile(String googleFolderIdParent, String contentType, //
                                          String customFileName, AbstractInputStreamContent uploadStreamContent) throws IOException {

        File fileMetadata = new File();
        fileMetadata.setName(customFileName);

        List<String> parents = Arrays.asList(googleFolderIdParent);
        fileMetadata.setParents(parents);
        //
        Drive driveService = GoogleDriveUtils.getDriveService();

        File file = driveService.files().create(fileMetadata, uploadStreamContent)
                .setFields("id, webContentLink, webViewLink, parents").execute();

        return file;
    }

    private static File _updateGoogleFile(String fileId, String googleFolderIdParent, String contentType, //
                                          String customFileName, AbstractInputStreamContent uploadStreamContent) throws IOException {

        File fileMetadata = new File();
        fileMetadata.setName(customFileName);

        List<String> parents = Arrays.asList(googleFolderIdParent);
        fileMetadata.setParents(parents);
        //
        Drive driveService = GoogleDriveUtils.getDriveService();

        File file = driveService.files().update(fileId, fileMetadata, uploadStreamContent)
                .setFields("id, webContentLink, webViewLink, parents").execute();

        return file;
    }

    // Create Google File from byte[]
    public static File createGoogleFile(String googleFolderIdParent, String contentType, //
                                        String customFileName, byte[] uploadData) throws IOException {
        //
        AbstractInputStreamContent uploadStreamContent = new ByteArrayContent(contentType, uploadData);
        //
        return _createGoogleFile(googleFolderIdParent, contentType, customFileName, uploadStreamContent);
    }

    // Create Google File from java.io.File
    public static File createGoogleFile(String googleFolderIdParent, String contentType, //
                                        String customFileName, java.io.File uploadFile) {
        try {
            //
            AbstractInputStreamContent uploadStreamContent = new FileContent(contentType, uploadFile);
            //
            return _createGoogleFile(googleFolderIdParent, contentType, customFileName, uploadStreamContent);

        } catch (Exception e) {
            // TODO: handle exception
            return null;
        }
    }

    // Create Google File from java.io.File
    public static File updateGoogleFile(String fileId, String googleFolderIdParent, String contentType, //
                                        String customFileName, java.io.File uploadFile) {
        try {
            //
            AbstractInputStreamContent uploadStreamContent = new FileContent(contentType, uploadFile);
            //
            return _updateGoogleFile(fileId, googleFolderIdParent, contentType, customFileName, uploadStreamContent);

        } catch (Exception e) {
            // TODO: handle exception
            return null;
        }
    }


    // Create Google File from InputStream
    public static File createGoogleFile(String googleFolderIdParent, String contentType, //
                                        String customFileName, InputStream inputStream) throws IOException {

        //
        AbstractInputStreamContent uploadStreamContent = new InputStreamContent(contentType, inputStream);
        //
        return _createGoogleFile(googleFolderIdParent, contentType, customFileName, uploadStreamContent);
    }


    //
    // Global instance of the scopes required by this quickstart. If modifying these
    // scopes, delete your previously saved credentials/ folder.
    //

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {

        java.io.File clientSecretFilePath = new java.io.File(CREDENTIALS_FOLDER, CLIENT_SECRET_FILE_NAME);

        if (!clientSecretFilePath.exists()) {
            throw new FileNotFoundException("Please copy " + CLIENT_SECRET_FILE_NAME //
                    + " to folder: " + CREDENTIALS_FOLDER.getAbsolutePath());
        }

        // Load client secrets.
        InputStream in = new FileInputStream(clientSecretFilePath);

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(CREDENTIALS_FOLDER))
                .setAccessType("offline").build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static final List<File> getGoogleFilesByName(String fileNameLike) throws IOException {

        Drive driveService = GoogleDriveUtils.getDriveService();

        String pageToken = null;
        List<File> list = new ArrayList<File>();

        String query = " name contains '" + fileNameLike + "' " //
                + " and mimeType != 'application/vnd.google-apps.folder' and trashed=false ";

        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive") //
                    // Fields will be assigned values: id, name, createdTime, mimeType
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType)")//
                    .setPageToken(pageToken).execute();
            for (File file : result.getFiles()) {
                list.add(file);
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        //
        return list;
    }
    public static void downloadFile(  String fileId, String outputPath)  throws IOException {
        Drive driveService = GoogleDriveUtils.getDriveService();
        java.io.File file = new java.io.File(outputPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        OutputStream outputStream = new FileOutputStream(file);
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        outputStream.close();
    }
    public static final List<File> getGoogleSubFolders(String googleFolderIdParent) throws IOException {

        Drive driveService = GoogleDriveUtils.getDriveService();

        String pageToken = null;
        List<File> list = new ArrayList<File>();

        String query = null;
        if (googleFolderIdParent == null) {
            query = " mimeType = 'application/vnd.google-apps.folder' and trashed=false  " //
                    + " and 'root' in parents";
        } else {
            query = " mimeType = 'application/vnd.google-apps.folder' and trashed=false " //
                    + " and '" + googleFolderIdParent + "' in parents";
        }

        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive") //
                    // Fields will be assigned values: id, name, createdTime
                    .setFields("nextPageToken, files(id, name, createdTime)")//
                    .setPageToken(pageToken).execute();
            for (File file : result.getFiles()) {
                list.add(file);
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        //
        return list;
    }

    public static final List<File> getGoogleSubFiles(String googleFolderIdParent) throws IOException {

        Drive driveService = GoogleDriveUtils.getDriveService();

        String pageToken = null;
        List<File> list = new ArrayList<File>();

        String query = null;
        if (googleFolderIdParent == null) {
            query = " mimeType != 'application/vnd.google-apps.folder' " //
                    + " and 'root' in parents and trashed=false ";
        } else {
            query = " mimeType != 'application/vnd.google-apps.folder' " //
                    + " and '" + googleFolderIdParent + "' in parents and trashed=false";
        }

        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive") //
                    // Fields will be assigned values: id, name, createdTime
                    .setFields("nextPageToken, files(id, name, createdTime , size)")//
                    .setPageToken(pageToken).execute();
            for (File file : result.getFiles()) {
                list.add(file);
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        //
        return list;
    }

    // com.google.api.services.drive.model.File
    public static final List<File> getGoogleRootFolders(String parent) {
        try {
            return getGoogleSubFolders(parent);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    // com.google.api.services.drive.model.File
    public static final List<File> getGoogleRootFiles(String parent) {
        try {
            return getGoogleSubFiles(parent);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        String folderRoot = "VETGO";
        String folderRootId = null;
        List<File> rootGoogleFolders = getGoogleRootFolders(null);
        for (File folderTemp1 : rootGoogleFolders) {
            System.out.println("Folder ID: " + folderTemp1.getId() + " --- Name: " + folderTemp1.getName() + folderTemp1.getSize() + folderTemp1.getCreatedTime());
        }
        Map<String, String> nameToIDMap = rootGoogleFolders.stream()
                .collect(Collectors.toMap(File::getName, File::getId, (oldValue, newValue) -> newValue));

        if(nameToIDMap.containsKey(folderRoot)){
            folderRootId = nameToIDMap.get(folderRoot);
        }else{
            // Create a Root Folder
            File folder = createGoogleFolder(null, "VETGO");
            System.out.println("Created folder with id= " + folder.getId());
            System.out.println("                    name= " + folder.getName());
            folderRootId = folder.getId();
            System.out.println("Done!");
        }
        System.out.println("ID nè!"+folderRootId);


           java.io.File uploadFile = new java.io.File("D:\\Project\\zalo-bot-v1\\src\\main\\resources\\zip\\nhat.doan.expo.zip");
//
//        // Create Google File:
//
        File googleFile = null;
        List<File> googleRootFolders = getGoogleFilesByName(uploadFile.getName());
        if(CollectionUtils.isNotEmpty(googleRootFolders)){
            File fileOld = googleRootFolders.stream().findFirst().get();
            downloadFile(fileOld.getId(),"D:\\Project\\zalo-bot-v1\\src\\main\\resources\\zip\\copy"+ java.io.File.separator +fileOld.getName() );

            //update file
              googleFile = updateGoogleFile(fileOld.getId(),folderRootId, "multipart/byteranges", uploadFile.getName(), uploadFile);
            System.out.println("update Google file!"+ googleFile);

        }else{
            //create file
              googleFile = createGoogleFile(folderRootId, "multipart/byteranges", uploadFile.getName(), uploadFile);
            System.out.println("Created Google file!");
            System.out.println("WebContentLink: " + googleFile.getWebContentLink());
            System.out.println("WebViewLink: " + googleFile.getWebViewLink());
        }
        System.out.println("Done!");







//
//        System.out.println("CREDENTIALS_FOLDER: " + CREDENTIALS_FOLDER.getAbsolutePath());
//
//        // 1: Create CREDENTIALS_FOLDER
//        if (!CREDENTIALS_FOLDER.exists()) {
//            CREDENTIALS_FOLDER.mkdirs();
//
//            System.out.println("Created Folder: " + CREDENTIALS_FOLDER.getAbsolutePath());
//            System.out.println("Copy file " + CLIENT_SECRET_FILE_NAME + " into folder above.. and rerun this class!!");
//            return;
//        }
//        java.io.File filejson = new java.io.File(CREDENTIALS_FOLDER.getAbsolutePath() + java.io.File.separator + CLIENT_SECRET_FILE_NAME);
//        if (filejson.exists() && filejson.length() > 0) {
//
//        } else {
//            FileUtils.copyFile(new java.io.File(CLIENT_SECRET_FILE_NAME), new java.io.File(CREDENTIALS_FOLDER.getAbsolutePath() + java.io.File.separator + CLIENT_SECRET_FILE_NAME));
//        }
//
//        // 2: Build a new authorized API client service.
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//
//        // 3: Read client_secret.json file & create Credential object.
//        Credential credential = getCredentials(HTTP_TRANSPORT);
//
//        // 5: Create Google Drive Service.
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential) //
//                .setApplicationName(APPLICATION_NAME).build();
//
//        // Print the names and IDs for up to 10 files.
//        FileList result = service.files().list().setPageSize(10).setFields("nextPageToken, files(id, name)").execute();
//        List<File> files = result.getFiles();
//        if (files == null || files.isEmpty()) {
//            System.out.println("No files found.");
//        } else {
//            System.out.println("Files:");
//            for (File file : files) {
//                System.out.printf("%s (%s)\n", file.getName(), file.getId());
//            }
//        }
//
//

//
//


    }


}