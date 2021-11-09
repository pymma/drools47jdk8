package org.drools.brms.server.util;
/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.fileupload.FileItem;
import org.drools.brms.client.common.AssetFormats;
import org.drools.brms.client.packages.PackageSnapshotView;
import org.drools.brms.server.ServiceImplementation;
import org.drools.brms.server.files.FileManagerUtils;
import org.drools.repository.AssetItem;
import org.drools.repository.PackageItem;
import org.drools.repository.RulesRepository;

public class FileManagerUtilsTest extends TestCase {
    
    public void testAttachFile() throws Exception {
        
        FileManagerUtils uploadHelper = new FileManagerUtils();
        
        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        uploadHelper.repository = repo;
        AssetItem item = repo.loadDefaultPackage().addAsset( "testUploadFile", "description" );
        FormData upload = new FormData();
        
        upload.setFile( new MockFile() );
        upload.setUuid( item.getUUID() );
        
        
        uploadHelper.attachFile( upload );
        
        AssetItem item2 = repo.loadDefaultPackage().loadAsset( "testUploadFile" );
        byte[] data = item2.getBinaryContentAsBytes();
        
        assertNotNull(data);
        assertEquals("foo bar", new String(data));
        assertEquals("foo.bar", item2.getBinaryContentAttachmentFileName());
    }
    
    public void testUploadXmlFile() throws Exception {
        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());

        repo.createPackage( "testUploadXmlFile", "comment" );
        repo.importRulesRepository( repo.dumpRepositoryXml() );
        assertTrue( repo.containsPackage( "testUploadXmlFile" ) );
    }
    
    public void testGetFilebyUUID() throws Exception {
        FileManagerUtils uploadHelper = new FileManagerUtils();
        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        uploadHelper.repository = repo;
        AssetItem item = repo.loadDefaultPackage().addAsset( "testGetFilebyUUID", "description" );
        FormData upload = new FormData();

        upload.setFile( new MockFile() );
        upload.setUuid( item.getUUID() );
        uploadHelper.attachFile( upload );


        ByteArrayOutputStream out = new ByteArrayOutputStream ();

        String filename = uploadHelper.loadFileAttachmentByUUID(item.getUUID(), out );

        assertNotNull(out.toByteArray());
        assertEquals("foo bar", new String(out.toByteArray()));
        assertEquals("foo.bar", filename);
    }
    
    public void testGetBinaryPackage() throws Exception {

        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        ServiceImplementation impl = new ServiceImplementation();
        impl.repository = repo;
        
        
        long before = System.currentTimeMillis();
        Thread.sleep( 20 );
        FileManagerUtils uploadHelper = new FileManagerUtils();

        uploadHelper.repository = repo;
        PackageItem pkg = repo.createPackage( "testGetBinaryPackageServlet", "" );
        pkg.updateHeader( "import java.util.List" );
        pkg.updateCompiledPackage( new ByteArrayInputStream("foo".getBytes()) );
        pkg.checkin( "" );

        assertTrue(before < uploadHelper.getLastModified( pkg.getName(), "LATEST" ));
        
        
        impl.createPackageSnapshot( pkg.getName(), "SNAPPY 1", false, "" );

        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String fileName = uploadHelper.loadBinaryPackage( pkg.getName(), PackageSnapshotView.LATEST_SNAPSHOT, true, out );
        assertEquals("testGetBinaryPackageServlet.pkg", fileName);
        byte[] file = out.toByteArray();
        assertNotNull(file);
        assertEquals("foo", new String(file));
        
        
        out = new ByteArrayOutputStream();
        fileName = uploadHelper.loadBinaryPackage( pkg.getName(),"SNAPPY 1", false, out );
        assertEquals("testGetBinaryPackageServlet_SNAPPY+1.pkg", fileName);
        file = out.toByteArray();
        assertNotNull(file);
        assertEquals("foo", new String(file));     
        
        Thread.sleep( 100 );
        impl.createPackageSnapshot( pkg.getName(), "SNAPX", false, "" );
        
        
        long lastMod = uploadHelper.getLastModified( pkg.getName(), "SNAPPY 1");
        assertTrue(pkg.getLastModified().getTimeInMillis() < lastMod);
        
        Thread.sleep( 100 );
        
        impl.createPackageSnapshot( pkg.getName(), "SNAPX", true, "yeah");
        
        long lastMod2 = uploadHelper.getLastModified( pkg.getName(), "SNAPX");
        assertTrue(lastMod < lastMod2);

        
    }
    
    public void testClassicDRLImport() throws Exception {
        FileManagerUtils fm = new FileManagerUtils();
        fm.repository = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        String drl = "package testClassicDRLImport\n import blah \n rule 'ola' \n when \n then \n end \n rule 'hola' \n when \n then \n end";
        InputStream in = new ByteArrayInputStream(drl.getBytes());        
        fm.importClassicDRL( in );
        
        PackageItem pkg = fm.repository.loadPackage( "testClassicDRLImport" );
        assertNotNull(pkg);
        
        
        List<AssetItem> rules = iteratorToList(pkg.getAssets());
        assertEquals(2, rules.size());
        
        final AssetItem rule1 = rules.get( 0 );
        assertEquals("ola", rule1.getName());
        assertNotNull(rule1.getContent());
        assertEquals(AssetFormats.DRL, rule1.getFormat());
        assertTrue(rule1.getContent().indexOf( "when" ) > -1);
        
        
        final AssetItem rule2 = rules.get( 1 );
        assertEquals("hola", rule2.getName());
        assertNotNull(rule2.getContent());
        assertEquals(AssetFormats.DRL, rule2.getFormat());
        assertTrue(rule2.getContent().indexOf( "when" ) > -1);
        
        assertNotNull(pkg.getHeader());
        assertTrue(pkg.getHeader().indexOf( "import" ) > -1);

        
        //now lets import an existing thing
        drl = "package testClassicDRLImport\n import should not see \n rule 'ola2' \n when \n then \n end \n rule 'hola' \n when \n then \n end";
        in = new ByteArrayInputStream(drl.getBytes());        
        fm.importClassicDRL( in );
        
        pkg = fm.repository.loadPackage( "testClassicDRLImport" );
        assertNotNull(pkg);
        
        //it should not overwrite this.
        assertTrue(pkg.getHeader().indexOf( "import should not see" ) == -1);
        
        rules = iteratorToList(pkg.getAssets());
        assertEquals(3, rules.size());
        
        
    }
    
    public void testClassicDRLImportWithDSL() throws Exception {
        FileManagerUtils fm = new FileManagerUtils();
        fm.repository = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        String drl = "package testClassicDRLImportDSL\n import blah \n expander goo \n rule 'ola' \n when \n then \n end \n rule 'hola' \n when \n then \n end";
        InputStream in = new ByteArrayInputStream(drl.getBytes());        
        fm.importClassicDRL( in );
        
        PackageItem pkg = fm.repository.loadPackage( "testClassicDRLImportDSL" );
        assertNotNull(pkg);
        
        
        List<AssetItem> rules = iteratorToList(pkg.getAssets());
        assertEquals(2, rules.size());
        
        final AssetItem rule1 = rules.get( 0 );
        assertEquals("ola", rule1.getName());
        assertNotNull(rule1.getContent());
        assertEquals(AssetFormats.DSL_TEMPLATE_RULE, rule1.getFormat());
        assertTrue(rule1.getContent().indexOf( "when" ) > -1);
        
        
        final AssetItem rule2 = rules.get( 1 );
        assertEquals("hola", rule2.getName());
        assertNotNull(rule2.getContent());
        assertEquals(AssetFormats.DSL_TEMPLATE_RULE, rule2.getFormat());
        assertTrue(rule2.getContent().indexOf( "when" ) > -1);
        
        assertNotNull(pkg.getHeader());
        assertTrue(pkg.getHeader().indexOf( "import" ) > -1);
        
    }
    

    private List iteratorToList(Iterator assets) {
        List<AssetItem> list = new ArrayList<AssetItem>();
        for ( Iterator iter = assets; iter.hasNext(); ) {
            AssetItem rule = (AssetItem) iter.next();
            list.add( rule );
        }
        return list;

    }
    
}

class MockFile implements FileItem {
    
    private static final long serialVersionUID = 400L;
    InputStream stream = new ByteArrayInputStream("foo bar".getBytes());
    
    public void setInputStream(InputStream is) throws IOException {
        stream.close();
        stream = is;
    }
    
    public void delete() {
    }

    public byte[] get() {

        return null;
    }

    public String getContentType() {

        return null;
    }

    public String getFieldName() {

        return null;
    }

    public InputStream getInputStream() throws IOException {
        return stream;
    }

    public String getName() {
        return "foo.bar";
    }

    public OutputStream getOutputStream() throws IOException {

        return null;
    }

    public long getSize() {
        return 0;
    }

    public String getString() {
        return null;
    }

    public String getString(String arg0) throws UnsupportedEncodingException {
        return null;
    }

    public boolean isFormField() {
        return false;
    }

    public boolean isInMemory() {
        return false;
    }

    public void setFieldName(String arg0) {

        
    }

    public void setFormField(boolean arg0) {

        
    }

    public void write(File arg0) throws Exception {
        
    }
    
}