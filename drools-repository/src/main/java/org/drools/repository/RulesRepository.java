package org.drools.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.apache.log4j.Logger;

/**
 * RulesRepository is the class that defines the bahavior for the JBoss Rules
 * (drools) rule repository based upon the JCR specification (JSR-170).
 * <p>
 * An instance of this class is capable of storing rules used by the JBoss Rule
 * engine. It also provides a versioning capability for rules. Rules can be
 * imported from specified files. The RulesRepository is also capable of storing
 * DSL content. Rules can be explicitly tied to a particular DSL node within the
 * repository, and this reference can either follow the head version, or a
 * specific version of the DSL node.
 * <p>
 * Rules can be tagged. Tags are stored in a separate area of the repository,
 * and can be added on demand. Rules can have 0 or more tags. Tags are intended
 * to help provide a means for searching for specific types of rules quickly,
 * even when they aren't all part of the same rulepackage.
 * <p>
 * Rules can be associated with 0 or 1 states. States are created in a seperate
 * area of the repository. States are intended to help track the progress of a
 * rule as it traverses its life- cycle. (e.g. draft, approved, deprecated,
 * etc.)
 * <p>
 * The RulesRepository provides versioning of rules, rule packages, and DSLs.
 * This versioning works in a strictly linear fashion, with one version having
 * at most 1 predecessor version (or none, if it is the first version), and at
 * most 1 successor version (or none, if it is the most recently checked-in
 * version). The JCR specification supports a more complicated versioning
 * system, and if there is sufficient demand, we can modify our versioning
 * scheme to be better aligned with JCR's versioning abilities.
 *
 * @author Ben Truitt
 * @author Fernando Meyer
 * @author Michael Neale
 */
public class RulesRepository {

    public static final String         DEFAULT_PACKAGE       = "defaultPackage";

    public static final String  DROOLS_URI            = "http://www.jboss.org/drools-repository/1.0";

    private static final Logger log                   = Logger.getLogger( RulesRepository.class );

    /**
     * The name of the rulepackage area of the repository
     */
    public final static String  RULE_PACKAGE_AREA     = "drools:package_area";

    /**
     * The name of the rulepackage area of the repository
     */
    public final static String  PACKAGE_SNAPSHOT_AREA = "drools:packagesnapshot_area";

    /**
     * The name of the tag area of the repository
     */
    public final static String  TAG_AREA              = "drools:tag_area";

    /**
     * The name of the state area of the repository
     */
    public final static String  STATE_AREA            = "drools:state_area";

    /**
     * The name of the rules repository within the JCR repository
     */
    public final static String  RULES_REPOSITORY_NAME = "drools:repository";

    private Session             session;

    /**
     * This requires a JCR session be setup, and the repository be configured.
     */
    public RulesRepository(
                           Session session) {
        this.session = session;
    }

    /**
     * Will add a node named 'nodeName' of type 'type' to 'parent' if such a
     * node does not already exist.
     *
     * @param parent
     *            the parent node to add the new node to
     * @param nodeName
     *            the name of the new node
     * @param type
     *            the type of the new node
     * @return a reference to the Node object that is created by the addition,
     *         or, if the node already existed, a reference to the pre-existant
     *         node.
     * @throws RulesRepositoryException
     */
    protected static Node addNodeIfNew(Node parent, String nodeName, String type) throws RulesRepositoryException {
        Node node;
        try {
            node = parent.getNode( nodeName );
        } catch ( PathNotFoundException e ) {
            // it doesn't exist yet, so create it
            try {
                log.debug( new StringBuilder().append( "Adding new node of type: " ).append( type ).append( " named: " ).append( nodeName ).append( " to parent node named " ).append( parent.getName() ) );

                node = parent.addNode( nodeName, type );
            } catch ( Exception e1 ) {
                log.error( "Caught Exception", e );
                throw new RulesRepositoryException( e1 );
            }
        } catch ( Exception e ) {
            log.error( "Caught Exception", e );
            throw new RulesRepositoryException( e );
        }
        return node;
    }

    /**
     * Explicitly logout of the underlying JCR repository.
     */
    public void logout() {
        this.session.logout();
    }

    private Node getAreaNode(String areaName) throws RulesRepositoryException {
        Node folderNode = null;
        int tries = 0;
        while ( folderNode == null && tries < 2 ) {
            try {
                tries++;
                folderNode = this.session.getRootNode().getNode( RULES_REPOSITORY_NAME + "/" + areaName );
            } catch ( PathNotFoundException e ) {
                if ( tries == 1 ) {
                    // hmm..repository must have gotten screwed up. set it
                    // up again
                    log.warn( "The repository appears to have become corrupted." );
                    throw new RulesRepositoryException( "Unable to get the main rule repo node. Repository is not setup correctly.",
                                                        e );
                } else {
                    log.error( "Unable to correct repository corruption" );
                }
            } catch ( Exception e ) {
                log.error( "Caught Exception", e );
                throw new RulesRepositoryException( "Caught exception " + e.getClass().getName(),
                                                    e );
            }
        }
        if ( folderNode == null ) {
            String message = "Could not get a reference to a node for " + RULES_REPOSITORY_NAME + "/" + areaName;
            log.error( message );
            throw new RulesRepositoryException( message );
        }
        return folderNode;
    }

    //    MN: This is kept for future reference showing how to tie references
    //    to a specific version when
    //    sharing assets.
    //
    //    /**
    //     * Adds a Rule node in the repository using the content specified,
    //     associating it with
    //     * the specified DSL node
    //     *
    //     * @param ruleName the name of the rule
    //     * @param lhsContent the lhs of the rule
    //     * @param rhsContent the rhs of the rule
    //     * @param dslItem the dslItem encapsulting the dsl node to associate
    //     this rule node with
    //     * @paaram followDslHead whether or not to follow the head revision of
    //     the dsl node
    //     * @return a RuleItem object encapsulating the node that gets added
    //     * @throws RulesRepositoryException
    //     */
    //    public RuleItem addRule(String ruleName, String ruleContent, DslItem
    //                            dslItem, boolean followDslHead) throws RulesRepositoryException {
    //        Node folderNode = this.getAreaNode(RULE_AREA);
    //
    //        try {
    //            //create the node - see section 6.7.22.6 of the spec
    //            Node ruleNode = folderNode.addNode(ruleName,
    //                                               RuleItem.RULE_NODE_TYPE_NAME);
    //
    //            ruleNode.setProperty(RuleItem.TITLE_PROPERTY_NAME, ruleName);
    //            ruleNode.setProperty(RuleItem.RULE_CONTENT_PROPERTY_NAME,
    //                                 ruleContent);
    //            ruleNode.setProperty(RuleItem.DESCRIPTION_PROPERTY_NAME, "");
    //            ruleNode.setProperty(RuleItem.FORMAT_PROPERTY_NAME,
    //                                 RuleItem.RULE_FORMAT);
    //
    //
    //            if(followDslHead) {
    //                ruleNode.setProperty(RuleItem.DSL_PROPERTY_NAME, dslItem.getNode());
    //            }
    //            else {
    //                //tie the ruleNode to specifically the current version of the dslNode
    //                ruleNode.setProperty(RuleItem.DSL_PROPERTY_NAME,
    //                                     dslItem.getNode().getBaseVersion());
    //            }
    //
    //            Calendar lastModified = Calendar.getInstance();
    //            ruleNode.setProperty(RuleItem.LAST_MODIFIED_PROPERTY_NAME,
    //                                 lastModified);
    //
    //            session.save();
    //
    //            try {
    //                ruleNode.checkin();
    //            }
    //            catch(UnsupportedRepositoryOperationException e) {
    //                String message = "Error: Caught
    //                    UnsupportedRepositoryOperationException when attempting to checkin
    //                    rule: " + ruleNode.getName() + ". Are you sure your JCR repository
    //                    supports versioning? ";
    //                        log.error(message + e);
    //                throw new RulesRepositoryException(message, e);
    //            }
    //
    //            return new RuleItem(this, ruleNode);
    //        }
    //        catch(Exception e) {
    //            log.error("Caught Exception", e);
    //            throw new RulesRepositoryException(e);
    //        }
    //    }
    //
    //
    //    /**
    //     * Adds a Rule node in the repository using the content specified
    //     *
    //     * @param ruleName the name of the rule
    //     * @param lhsContent the lhs of the rule
    //     * @param rhsContent the rhs of the rule
    //     * @return a RuleItem object encapsulating the node that gets added
    //     * @throws RulesRepositoryException
    //     */
    //    public RuleItem addRule(String ruleName, String ruleContent) throws
    //    RulesRepositoryException {
    //        Node folderNode = this.getAreaNode(RULE_AREA);
    //
    //        try {
    //            //create the node - see section 6.7.22.6 of the spec
    //            Node ruleNode = folderNode.addNode(ruleName,
    //                                               RuleItem.RULE_NODE_TYPE_NAME);
    //
    //            ruleNode.setProperty(RuleItem.TITLE_PROPERTY_NAME, ruleName);
    //
    //
    //            ruleNode.setProperty(RuleItem.DESCRIPTION_PROPERTY_NAME, "");
    //            ruleNode.setProperty(RuleItem.FORMAT_PROPERTY_NAME,
    //                                 RuleItem.RULE_FORMAT);
    //            ruleNode.setProperty(RuleItem.RULE_CONTENT_PROPERTY_NAME,
    //                                 ruleContent);
    //
    //            ruleNode.setProperty( VersionableItem.CHECKIN_COMMENT, "Initial" );
    //
    //
    //            Calendar lastModified = Calendar.getInstance();
    //            ruleNode.setProperty(RuleItem.LAST_MODIFIED_PROPERTY_NAME,
    //                                 lastModified);
    //
    //            session.save();
    //
    //            try {
    //                ruleNode.checkin();
    //            }
    //            catch(UnsupportedRepositoryOperationException e) {
    //                String message = "Error: Caught
    //                    UnsupportedRepositoryOperationException when attempting to checkin
    //                    rule: " + ruleNode.getName() + ". Are you sure your JCR repository
    //                    supports versioning? ";
    //                        log.error(message + e);
    //                throw new RulesRepositoryException(message, e);
    //            }
    //
    //            return new RuleItem(this, ruleNode);
    //        }
    //        catch(Exception e) {
    //            log.error("Caught Exception", e);
    //            throw new RulesRepositoryException(e);
    //        }
    //    }

    /**
     * This will copy an assets content to the new location.
     * @return the UUID of the new asset.
     */
    public String copyAsset(String uuidSource, String destinationPackage, String destinationName) {
        try {
            AssetItem source = loadAssetByUUID( uuidSource );
            String sourcePath = source.getNode().getPath();

            String destPath = this.getAreaNode( RULE_PACKAGE_AREA ).getPath() + "/" + destinationPackage + "/" + PackageItem.ASSET_FOLDER_NAME + "/" + destinationName;
            this.session.getWorkspace().copy( sourcePath, destPath );
            AssetItem dest = loadPackage( destinationPackage ).loadAsset( destinationName );
//            if (dest.getContent() != null ) {
//                dest.updateContent( dest.getContent().replaceAll( source.getName(), dest.getName() ) );
//            }

            dest.updateStringProperty( destinationPackage, AssetItem.PACKAGE_NAME_PROPERTY );
            dest.node.setProperty( AssetItem.VERSION_NUMBER_PROPERTY_NAME, 0 );
            dest.checkin( "Copied from " + source.getPackageName() + "/" + source.getName() );
            return dest.getUUID();
        } catch ( RepositoryException e ) {
            log.error( "Unable to copy asset.", e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * Loads a RulePackage for the specified package name. Will throw an
     * exception if the specified rule package does not exist.
     *
     * @param name
     *            the name of the package to load
     * @return a RulePackageItem object
     */
    public PackageItem loadPackage(String name) throws RulesRepositoryException {
        try {
            Node folderNode = this.getAreaNode( RULE_PACKAGE_AREA );
            Node rulePackageNode = folderNode.getNode( name );

            return new PackageItem( this,
                                    rulePackageNode );
        } catch ( RepositoryException e ) {
            log.error( "Unable to load a rule package. ", e );

            throw new RulesRepositoryException( "Unable to load a rule package. ",
                                                e );

        }
    }

    /**
     * This returns true if the repository contains the specified package name.
     */
    public boolean containsPackage(String name) {
        Node folderNode = this.getAreaNode( RULE_PACKAGE_AREA );
        try {
            return folderNode.hasNode( name );
        } catch ( RepositoryException e ) {
            throw new RulesRepositoryException( e );
        }
    }


    public PackageItem loadPackageSnapshot(String packageName, String snapshotName) {
        try {
            Node n = this.getAreaNode( PACKAGE_SNAPSHOT_AREA ).getNode( packageName ).getNode( snapshotName );
            return new PackageItem( this,
                                    n );
        } catch ( RepositoryException e ) {
            log.error( e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * This will copy the package to the snapshot area. Creating a copy for
     * deployment, etc.
     */
    public void createPackageSnapshot(String packageName, String snapshotName) {
        log.debug( "Creating snapshot for [" + packageName + "] called [" + snapshotName + "]" );
        try {
            Node snaps = this.getAreaNode( PACKAGE_SNAPSHOT_AREA );

            if ( !snaps.hasNode( packageName ) ) {
                snaps.addNode( packageName, "nt:folder" );
                save();
            }

            Node pkgSnaps = snaps.getNode( packageName );

            String newName = pkgSnaps.getPath() + "/" + snapshotName;

            Node folderNode = this.getAreaNode( RULE_PACKAGE_AREA );
            Node rulePackageNode = folderNode.getNode( packageName );

            String source = rulePackageNode.getPath();

            this.session.getWorkspace().copy( source, newName );

        } catch ( RepositoryException e ) {
            log.error( "Unable to create snapshot", e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * This will remove the specified snapshot.
     */
    public void removePackageSnapshot(String packageName, String snapshotName) {
        log.debug( "Removing snapshot for [" + packageName + "] called [" + snapshotName + "]" );
        try {
            Node snaps = this.getAreaNode( PACKAGE_SNAPSHOT_AREA );

            if ( !snaps.hasNode( packageName ) ) {
                throw new RulesRepositoryException( "The package " + packageName + " does not have any snapshots." );
            }

            Node pkgSnaps = snaps.getNode( packageName );

            if ( pkgSnaps.hasNode( snapshotName ) ) {
                pkgSnaps.getNode( snapshotName ).remove();
            }

            save();
        } catch ( RepositoryException e ) {
            log.error( "Unable to remove snapshot", e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * Copies a snapshot to the new location/label.
     *
     * @param packageName
     *            The name of the package.
     * @param snapshotName
     *            The label of the source snapshot
     * @param newName
     *            The new label. The old one is left intact.
     */
    public void copyPackageSnapshot(String packageName, String snapshotName, String newName) {
        log.debug( "Creating snapshot for [" + packageName + "] called [" + snapshotName + "]" );
        try {
            Node snaps = this.getAreaNode( PACKAGE_SNAPSHOT_AREA );

            Node pkgSnaps = snaps.getNode( packageName );

            Node sourceNode = pkgSnaps.getNode( snapshotName );

            String destinationPath = pkgSnaps.getPath() + "/" + newName;

            this.session.getWorkspace().copy( sourceNode.getPath(), destinationPath );
        } catch ( RepositoryException e ) {
            log.error( "Unable to create snapshot", e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * This will return or create the default package for rules that have no
     * home yet.
     */
    public PackageItem loadDefaultPackage() throws RulesRepositoryException {
        Node folderNode = this.getAreaNode( RULE_PACKAGE_AREA );
        try {
            if ( folderNode.hasNode( DEFAULT_PACKAGE ) ) {
                return loadPackage( DEFAULT_PACKAGE );
            } else {
                return createPackage( DEFAULT_PACKAGE, "" );
            }
        } catch ( RepositoryException e ) {
            throw new RulesRepositoryException( e );
        }

    }

    /**
     * Similar to above. Loads a RulePackage for the specified uuid.
     *
     * @param uuid
     *            the uuid of the package to load
     * @return a RulePackageItem object
     * @throws RulesRepositoryException
     */
    public PackageItem loadPackageByUUID(String uuid) throws RulesRepositoryException {
        try {
            Node rulePackageNode = this.session.getNodeByUUID( uuid );
            return new PackageItem( this,
                                    rulePackageNode );
        } catch ( Exception e ) {
            log.error( "Unable to load a rule package by UUID. ", e );
            if ( e instanceof RuntimeException ) {
                throw (RuntimeException) e;
            } else {
                throw new RulesRepositoryException( "Unable to load a rule package. ",
                                                    e );
            }
        }
    }

    /**
     * This will restore the historical version, save, and check it in as a new
     * version with the given comment.
     *
     * @param versionToRestore
     * @param headVersion
     * @param comment
     */
    public void restoreHistoricalAsset(AssetItem versionToRestore, AssetItem headVersion, String comment) {

        long oldVersionNumber = headVersion.getVersionNumber();

        Version v = (Version) versionToRestore.getNode();
        try {
            headVersion.getNode().restore( v, true );
            AssetItem newHead = loadAssetByUUID( headVersion.getUUID() );
            newHead.checkout();
            newHead.getNode().setProperty( VersionableItem.VERSION_NUMBER_PROPERTY_NAME, oldVersionNumber );
            newHead.checkin( comment );
        } catch ( RepositoryException e ) {
            log.error( "Unable to restore version of asset.", e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * Loads a rule by its UUID (generally the fastest way to load something).
     */
    public AssetItem loadAssetByUUID(String uuid) {
        try {
            Node rulePackageNode = this.session.getNodeByUUID( uuid );
            return new AssetItem( this,
                                  rulePackageNode );
        } catch ( RepositoryException e ) {
            log.error( "Unable to load a rule asset by UUID.", e );
            throw new RulesRepositoryException( e );
        }

    }

    /**
     * Adds a package to the repository.
     *
     * @param name
     *            what to name the node added
     * @param description
     *            what description to use for the node
     * @return a PackageItem, encapsulating the created node
     * @throws RulesRepositoryException
     */
    public PackageItem createPackage(String name, String description) throws RulesRepositoryException {
        Node folderNode = this.getAreaNode( RULE_PACKAGE_AREA );

        try {
            // create the node - see section 6.7.22.6 of the spec
            Node rulePackageNode = folderNode.addNode( name, PackageItem.RULE_PACKAGE_TYPE_NAME );

            rulePackageNode.addNode( PackageItem.ASSET_FOLDER_NAME, "drools:versionableAssetFolder" );

            rulePackageNode.setProperty( PackageItem.TITLE_PROPERTY_NAME, name );

            rulePackageNode.setProperty( AssetItem.DESCRIPTION_PROPERTY_NAME, description );
            rulePackageNode.setProperty( AssetItem.FORMAT_PROPERTY_NAME, PackageItem.PACKAGE_FORMAT );
            rulePackageNode.setProperty( PackageItem.CREATOR_PROPERTY_NAME, this.session.getUserID() );

            Calendar lastModified = Calendar.getInstance();
            rulePackageNode.setProperty( PackageItem.LAST_MODIFIED_PROPERTY_NAME, lastModified );

            PackageItem item = new PackageItem( this,
                                                rulePackageNode );
            item.checkin( "Initial" );

            return item;
        } catch ( ItemExistsException e ) {
            throw new RulesRepositoryException( "A package name must be unique.",
                                                e );
        } catch ( RepositoryException e ) {
            log.error( "Error when creating a new rule package", e );
            throw new RulesRepositoryException( e );
        }

    }

    /**
     * Gets a StateItem for the specified state name. If a node for the
     * specified state does not yet exist, one is first created.
     *
     * @param name
     *            the name of the state to get
     * @return a StateItem object encapsulating the retreived node
     * @throws RulesRepositoryException
     */
    public StateItem getState(String name) throws RulesRepositoryException {
        try {
            Node folderNode = this.getAreaNode( STATE_AREA );
            if ( !folderNode.hasNode( name ) ) {
                throw new RulesRepositoryException( "The state called [" + name + "] does not exist." );
            }
            Node stateNode = folderNode.getNode( name );// RulesRepository.addNodeIfNew(folderNode,
            // name,
            // StateItem.STATE_NODE_TYPE_NAME);
            return new StateItem( this,
                                  stateNode );
        } catch ( Exception e ) {
            log.error( e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * Create a status node of the given name.
     */
    public StateItem createState(String name) {
        try {
            Node folderNode = this.getAreaNode( STATE_AREA );
            Node stateNode = RulesRepository.addNodeIfNew( folderNode, name, StateItem.STATE_NODE_TYPE_NAME );
            log.debug( "Created the status [" + name + "]" );
            return new StateItem( this,
                                  stateNode );
        } catch ( Exception e ) {
            log.error( e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * This will return a category for the given category path.
     *
     * @param tagName
     *            the name of the tag to get. If the tag to get is within a
     *            heirarchy of tag nodes, specify the full path to the tag node
     *            of interest (e.g. if you want to get back 'child-tag', use
     *            "parent-tag/child-tag")
     * @return a TagItem object encapsulating the node for the tag in the
     *         repository
     * @throws RulesRepositoryException
     */
    public CategoryItem loadCategory(String tagName) throws RulesRepositoryException {
        if ( tagName == null || "".equals( tagName ) ) {
            throw new RuntimeException( "Empty category name not permitted." );
        }

        try {
            Node folderNode = this.getAreaNode( TAG_AREA );
            Node tagNode = folderNode;

            StringTokenizer tok = new StringTokenizer( tagName,
                                                       "/" );
            while ( tok.hasMoreTokens() ) {
                String currentTagName = tok.nextToken();
                tagNode = folderNode.getNode( currentTagName );
                // MN was this: RulesRepository.addNodeIfNew(folderNode,
                // currentTagName, CategoryItem.TAG_NODE_TYPE_NAME);
                folderNode = tagNode;
            }

            return new CategoryItem( this,
                                     tagNode );
        } catch ( RepositoryException e ) {
            if ( e instanceof PathNotFoundException ) {
                throw new RulesRepositoryException( "Unable to load the category : [" + tagName + "] does not exist.",
                                                    e );
            }
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * This will retrieve a list of RuleItem objects - that are allocated to the
     * provided category. Only the latest versions of each RuleItem will be
     * returned (you will have to delve into the rules deepest darkest history
     * yourself... mahahahaha).
     */
    public List findAssetsByCategory(String categoryTag, boolean seekArchivedAsset) throws RulesRepositoryException {

        CategoryItem item = this.loadCategory( categoryTag );
        List results = new ArrayList();
        try {
            PropertyIterator it = item.getNode().getReferences();

            while ( it.hasNext() ) {
                Property ruleLink = (Property) it.next();
                Node parentNode = ruleLink.getParent();
                if ( isNotSnapshot( parentNode ) && parentNode.getPrimaryNodeType().getName().equals( AssetItem.RULE_NODE_TYPE_NAME ) ) {
                    if ( seekArchivedAsset || !parentNode.getProperty( AssetItem.CONTENT_PROPERTY_ARCHIVE_FLAG ).getBoolean() ) results.add( new AssetItem( this,
                                                                                                                                                            parentNode ) );
                }
            }
            return results;
        } catch ( RepositoryException e ) {
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * TODO: Comment
     */
    public List findAssetsByCategory(String categoryTag) throws RulesRepositoryException {
        return this.findAssetsByCategory( categoryTag, false );
    }

    /**
     * TODO: comment
     * @return
     * @throws IOException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public byte[] exportRulesRepository() throws IOException,
                                         PathNotFoundException,
                                         RepositoryException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream( bout );

        zout.putNextEntry( new ZipEntry( "repository_export.xml" ) );
        zout.write( dumpRepositoryXml() );
        zout.closeEntry();
        zout.finish();
        return bout.toByteArray();
    }

    public byte[] dumpRepositoryXml() throws PathNotFoundException,
                                     IOException,
                                     RepositoryException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        session.refresh( false );
        session.exportSystemView( "/" + RULES_REPOSITORY_NAME, byteOut, false, false );
        return byteOut.toByteArray();
    }

    /**
     *
     * @param byteArray
     */
    public void importRulesRepository(byte[] byteArray) {
        try {
            new RulesRepositoryAdministrator( this.session ).clearRulesRepository();
            this.session.getWorkspace().importXML( "/", new ByteArrayInputStream( byteArray ), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW );
            session.save();
        } catch ( RepositoryException e ) {
            e.printStackTrace();
            throw new RulesRepositoryException();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param parentNode
     * @return
     * @throws RepositoryException
     */
    boolean isNotSnapshot(Node parentNode) throws RepositoryException {
        return parentNode.getPath().indexOf( PACKAGE_SNAPSHOT_AREA ) == -1;
    }

    /**
     * @return an Iterator which will provide RulePackageItem's. This will show
     *         ALL the packages, only returning latest versions, by default.
     */
    public Iterator listPackages() {
        Node folderNode = this.getAreaNode( RULE_PACKAGE_AREA );

        try {
            if ( !folderNode.hasNode( DEFAULT_PACKAGE ) ) {
                createPackage( DEFAULT_PACKAGE, "The default rule package" );
                folderNode = this.getAreaNode( RULE_PACKAGE_AREA );
            }
            return new PackageIterator( this,
                                        folderNode.getNodes() );
        } catch ( RepositoryException e ) {
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * @return The JCR session that this repository is using.
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * Save any pending changes.
     */
    public void save() {
        try {
            this.session.save();
        } catch ( Exception e ) {
            if ( e instanceof RuntimeException ) {
                throw (RuntimeException) e;
            } else {
                throw new RulesRepositoryException( e );
            }
        }

    }

    /**
     * This moves a rule asset from one package to another, preserving history
     * etc etc.
     *
     * @param newPackage
     *            The destination package.
     * @param uuid
     *            The UUID of the rule
     * @param explanation
     *            The reason (which will be added as the checkin message).
     */
    public void moveRuleItemPackage(String newPackage, String uuid, String explanation) {
        try {
            AssetItem item = loadAssetByUUID( uuid );

            PackageItem destPkg = loadPackage( newPackage );

            String sourcePath = item.node.getPath();
            String destPath = destPkg.node.getPath() + "/" + PackageItem.ASSET_FOLDER_NAME + "/" + item.getName();

            this.session.move( sourcePath, destPath );

            item.checkout();
            item.node.setProperty( AssetItem.PACKAGE_NAME_PROPERTY, newPackage );

            item.checkin( explanation );

        } catch ( RepositoryException e ) {
            throw new RulesRepositoryException( e );
        }

    }

    /**
     * This will rename an assset and apply the change immediately.
     * @return the UUID of the new asset
     */
    public String renameAsset(String uuid, String newAssetName) {
        try {
            AssetItem itemOriginal = loadAssetByUUID( uuid );
            log.info( "Renaming asset: " + itemOriginal.getNode().getPath() + " to " + newAssetName );
            Node node = itemOriginal.getNode();
            String sourcePath = node.getPath();
            String destPath = node.getParent().getPath() + "/" + newAssetName;
            this.session.move( sourcePath, destPath );


            itemOriginal.updateTitle( newAssetName );
            itemOriginal.checkin( "Renamed asset " +  itemOriginal.getName());
            return itemOriginal.getUUID();
        } catch (RepositoryException e) {
            log.error( e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * This will rename a package and apply the change immediately.
     * @return the UUID of the package
     */
    public String renamePackage(String uuid, String newPackageName) {
        try {
            PackageItem itemOriginal = loadPackageByUUID( uuid );
            log.info( "Renaming package: " + itemOriginal.getNode().getPath() + " to " + newPackageName );
            Node node = itemOriginal.getNode();
            String sourcePath = node.getPath();
            String destPath = node.getParent().getPath() + "/" + newPackageName;
            this.session.move( sourcePath, destPath );


            itemOriginal.updateTitle( newPackageName );
            itemOriginal.checkin( "Renamed package " +  itemOriginal.getName());

            PackageItem newPkg = loadPackage(newPackageName);

            for ( Iterator iter = newPkg.getAssets(); iter.hasNext(); ) {
                AssetItem as = (AssetItem) iter.next();
                as.updateStringProperty( newPackageName, AssetItem.PACKAGE_NAME_PROPERTY );
            }

            save();

            return itemOriginal.getUUID();
        } catch (RepositoryException e) {
            log.error( e );
            throw new RulesRepositoryException( e );
        }
    }

    /**
     * Return a list of the snapshots available for the given package name.
     */
    public String[] listPackageSnapshots(String packageName) {
        Node snaps = this.getAreaNode( PACKAGE_SNAPSHOT_AREA );
        try {
            if ( !snaps.hasNode( packageName ) ) {
                return new String[0];
            } else {
                List result = new ArrayList();
                NodeIterator it = snaps.getNode( packageName ).getNodes();
                while ( it.hasNext() ) {
                    Node element = (Node) it.next();
                    result.add( element.getName() );
                }
                return (String[]) result.toArray( new String[result.size()] );
            }
        } catch ( RepositoryException e ) {
            throw new RulesRepositoryException( e );
        }
    }

    public AssetItemIterator findArchivedAssets() {
        try {

            String sql = "SELECT " + AssetItem.TITLE_PROPERTY_NAME + ", " + AssetItem.DESCRIPTION_PROPERTY_NAME + ", " + AssetItem.CONTENT_PROPERTY_ARCHIVE_FLAG + " FROM " + AssetItem.RULE_NODE_TYPE_NAME;
            sql += " WHERE ";
            sql += " jcr:path LIKE '/" + RULES_REPOSITORY_NAME + "/" + RULE_PACKAGE_AREA + "/%'";
            sql += " AND " + AssetItem.CONTENT_PROPERTY_ARCHIVE_FLAG + " = 'true'";

            Query q = this.session.getWorkspace().getQueryManager().createQuery( sql, Query.SQL );

            QueryResult res = q.execute();

            return new AssetItemIterator( res.getNodes(),
                                          this );
        } catch ( RepositoryException e ) {
            System.out.println( e.getMessage() );
            throw new RulesRepositoryException( e );
        }
    }


    /**
     * This will search assets, looking for matches against the name.
     */
    public AssetItemIterator findAssetsByName(String name, boolean seekArchived) {
        try {

            String sql = "SELECT " + AssetItem.TITLE_PROPERTY_NAME + ", " + AssetItem.DESCRIPTION_PROPERTY_NAME + ", " + AssetItem.CONTENT_PROPERTY_ARCHIVE_FLAG + " FROM " + AssetItem.RULE_NODE_TYPE_NAME;
            sql += " WHERE " + AssetItem.TITLE_PROPERTY_NAME + " LIKE '" + name + "'";
            sql += " AND jcr:path LIKE '/" + RULES_REPOSITORY_NAME + "/" + RULE_PACKAGE_AREA + "/%'";

            if ( seekArchived == false ) {
                sql += " AND " + AssetItem.CONTENT_PROPERTY_ARCHIVE_FLAG + " = 'false'";
            }

            Query q = this.session.getWorkspace().getQueryManager().createQuery( sql, Query.SQL );

            QueryResult res = q.execute();

            return new AssetItemIterator( res.getNodes(),
                                          this );
        } catch ( RepositoryException e ) {
            System.out.println( e.getMessage() );
            throw new RulesRepositoryException( e );
        }
    }

    public AssetItemIterator findAssetsByName(String name) {
        return this.findAssetsByName( name, false );
    }

    /**
     * @return A list of statii in the system.
     */
    public StateItem[] listStates() {
        List states = new ArrayList();
        NodeIterator it;
        try {
            it = this.getAreaNode( STATE_AREA ).getNodes();

            while ( it.hasNext() ) {
                states.add( new StateItem( this,
                                           it.nextNode() ) );
            }
        } catch ( RepositoryException e ) {
            log.error( e );
            throw new RulesRepositoryException( e );
        }
        return (StateItem[]) states.toArray( new StateItem[states.size()] );
    }

    /**
     * Copy a package to the target name.
     */
    public void copyPackage(String sourcePackageName, String destPackageName) {
        PackageItem source = loadPackage( sourcePackageName );
        String sourcePath;

        try {
            sourcePath = source.getNode().getPath();

            String destPath = source.getNode().getParent().getPath() + "/" + destPackageName;
            if ( this.getAreaNode( RULE_PACKAGE_AREA ).hasNode( destPackageName ) ) {
                throw new RulesRepositoryException( "Destination already exists." );
            }
            this.session.getWorkspace().copy( sourcePath, destPath );

            PackageItem newPkg = loadPackage( destPackageName );

            for ( Iterator iter = newPkg.getAssets(); iter.hasNext(); ) {
                AssetItem as = (AssetItem) iter.next();
                as.updateStringProperty( destPackageName, AssetItem.PACKAGE_NAME_PROPERTY );
            }

            save();

        } catch ( RepositoryException e ) {
            log.error( e );
            throw new RulesRepositoryException(e);
        }

    }
}