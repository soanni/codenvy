/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.ldap;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.eclipse.che.api.core.util.CustomPortService;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;
import static org.eclipse.che.commons.lang.IoUtil.deleteRecursive;

/**
 * An embedded ldap server.
 *
 * @author Yevhenii Voevodin
 */
public class MyLdapServer {

    private static final String            ADMIN_CN        = "admin";
    private static final String            ADMIN_PWD       = "password";
    private static final String            DEFAULT_BASE_DN = "dc=codenvy,dc=com";
    private static final CustomPortService PORT_SERVICE    = new CustomPortService(8000, 10000);

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates and returns a new instance of {@link MyLdapServer} with default configuration.
     *
     * <ul>
     * <li>partition id - 'codenvy'</li>
     * <li>partition dn - '{@value #DEFAULT_BASE_DN}'</li>
     * <li>allowing anonymous access</li>
     * <li>using temporary generated working directory</li>
     * </ul>
     */
    public static MyLdapServer newDefaultServer() throws Exception {
        return MyLdapServer.builder()
                           .setPartitionId("codenvy")
                           .allowAnonymousAccess()
                           .setPartitionDn(DEFAULT_BASE_DN)
                           .useTmpWorkingDir()
                           .build();
    }

    private LdapServer       ldapServer;
    private DirectoryService service;
    private File             workingDir;
    private String           url;
    private int              port;
    private DN               baseDn;

    public MyLdapServer(File workingDir,
                        String partitionDn,
                        String partitionId,
                        int port,
                        boolean enableChangelog,
                        boolean allowAnonymousAccess,
                        long maxSizeLimit) throws Exception {
        requireNonNull(partitionDn, "Required non-null partition dn");
        requireNonNull(partitionId, "Required non-null partition id");
        this.workingDir = workingDir;
        this.baseDn = new DN(partitionDn);
        this.port = port > 0 ? port : PORT_SERVICE.acquire();
        this.url = "ldap://localhost:" + this.port;
        ldapServer = new LdapServer();
        ldapServer.setTransports(new TcpTransport(this.port));
        if (maxSizeLimit > 0) {
            ldapServer.setMaxSizeLimit(maxSizeLimit);
        }
        service = initDirectoryService(workingDir,
                                       partitionId,
                                       partitionDn,
                                       enableChangelog,
                                       allowAnonymousAccess);
        ldapServer.setDirectoryService(service);
    }

    public static void main(String[] arg) throws Exception {
        String BASE_DN = "dc=codenvy,dc=com";
        MyLdapServer server = MyLdapServer.builder()
                                          .setPort(8235)
                                          .setPartitionId("codenvy")
                                          .allowAnonymousAccess()
                                          .setPartitionDn(BASE_DN)
                                          .useTmpWorkingDir()
                                          .setMaxSizeLimit(1000)
                                          .build();
        server.start();
        for (int i = 0; i < 2; i++) {
            String password = NameGenerator.generate("pwd", 20);

            server.addDefaultLdapUser(i,
                                      Pair.of("givenName", "test-user-first-name" + i),
                                      Pair.of("sn", "test-user-last-name"),
                                      //Pair.of("userPassword", passwordEncryptor.encrypt(password).getBytes()),
                                      Pair.of("userPassword", password.getBytes()),
                                      Pair.of("telephoneNumber", "00000000" + i));

        }
        Thread.sleep(1000000);
    }

    /**
     * Starts ldap server, please not that all the schema modifications
     * should be performed before server is started.
     */
    public void start() throws Exception {
        ldapServer.start();
    }

    /**
     * Stops ldap server, releasing all the acquired resources.
     */
    public void stop() {
        ldapServer.stop();
        PORT_SERVICE.release(port);
        deleteRecursive(workingDir);
    }

    /** Returns this server url. */
    public String getUrl() {
        return url;
    }

    /**
     * Creates a new entry in base dn.
     *
     * <p>E.g. if {@code base_dn} is set to <i>dc=codenvy,dc=com</i> for {@code name=cn}
     * and {@code value=admin} the entity dn will be <i>cn=admin,dc=codenvy,dc=com</i>.
     *
     * <p>To add the entity attributes to directory service use {@link #addEntry(ServerEntry)}.
     *
     * @param name
     *         the name of the dn attribute e.g. 'cn'
     * @param value
     *         the value of the attribute e.g. 'admin'
     * @return a new instance of {@link ServerEntry}
     * @throws Exception
     *         when any error occurs
     */
    public ServerEntry newEntry(String name, String value) throws Exception {
        return service.newEntry(new DN(name + '=' + value + ',' + baseDn.toString()));
    }

    /**
     * Adds the {@code entry} to this directory service.
     *
     * @throws Exception
     *         when the {@code entry} can't be added
     */
    public void addEntry(ServerEntry entry) throws Exception {
        service.getAdminSession().add(entry);
    }

    /**
     * Adds a new user which matches the default schema pattern, which is:
     *
     * <ul>
     * <li>objectClass=inetOrgPerson</li>
     * <li>rdn - uid={id}</li>
     * <li>cn={name}</li>
     * <li>mail={mail}</li>
     * <li>sn={@literal <none>}</li>
     * <li>other.foreach(pair -> {pair.first}={pair.second})</li>
     * </ul>
     *
     * @return newly created and added entry instance
     * @throws Exception
     *         when any error occurs
     */
    public ServerEntry addDefaultLdapUser(String id, String name, String mail, Pair... other) throws Exception {
        final ServerEntry entry = newEntry("uid", id);
        entry.put("objectClass", "inetOrgPerson");
        entry.put("uid", id);
        entry.put("cn", name);
        entry.put("mail", mail);
        entry.put("sn", "<none>");
        for (Pair pair : other) {
            if (pair.second instanceof String) {
                entry.put(pair.first.toString(), (String)pair.second);
            } else if (pair.second instanceof byte[]) {
                entry.put(pair.first.toString(), (byte[])pair.second);
            } else {
                entry.put(pair.first.toString(), pair.second.toString());
            }
        }
        addEntry(entry);
        return entry;
    }

    /**
     * Simplifies creation of test user entry by generating id, name and mail
     * based on given {@code idx}.
     *
     * @see #addDefaultLdapUser(String, String, String, Pair[])
     */
    public ServerEntry addDefaultLdapUser(int idx, Pair... other) throws Exception {
        return addDefaultLdapUser("id" + idx, "name" + idx, "mail" + idx, other);
    }

    /**
     * Creates a new group which matches default schema pattern, which is:
     *
     * <ul>
     * <li>objectClass=groupOfNames</li>
     * <li>rdn - ou={name}</li>
     * <li>cn={name}</li>
     * <li>members.foreach(m -> member={m})</li>
     * </ul>
     *
     * @param name
     *         a name of a group
     * @return newly created and added group entry
     * @throws Exception
     *         when any error occurs
     */
    public ServerEntry addDefaultLdapGroup(String name, List<String> members) throws Exception {
        final ServerEntry group = newEntry("ou", name);
        group.put("objectClass", "top", "groupOfNames");
        group.put("cn", name);
        group.put("ou", name);
        for (String member : members) {
            group.add("member", member);
        }
        addEntry(group);
        return group;
    }

    /** Returns service instance of this server. */
    public DirectoryService getService() {
        return service;
    }

    /** Returns admin dn, it can be used for authentication. */
    public String getAdminDn() {
        return "cn=" + ADMIN_CN + ',' + baseDn.toString();
    }

    /** Returns admin password, it can be used for authentication. */
    public String getAdminPassword() {
        return ADMIN_PWD;
    }

    private static DirectoryService initDirectoryService(File workingDir,
                                                         String partitionId,
                                                         String partitionDn,
                                                         boolean enableChangelog,
                                                         boolean allowAnonymousAccess) throws Exception {
        final DirectoryService service = new DefaultDirectoryService();
        service.setShutdownHookEnabled(false);
        service.setAllowAnonymousAccess(allowAnonymousAccess);
        service.getChangeLog().setEnabled(enableChangelog);
        service.setWorkingDirectory(workingDir);

        // Init schema manager
        SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();
        LdifPartition ldifPartition = new LdifPartition();
        String workDirectory = service.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory(workDirectory + "/schema");
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(new File(workDirectory));
        extractor.extractOrCopy(true);
        schemaPartition.setWrappedPartition(ldifPartition);
        SchemaLoader schemaLoader = new LdifSchemaLoader(new File(workDirectory + "/schema"));
        SchemaManager schemaManager = new DefaultSchemaManager(schemaLoader);
        schemaPartition.setSchemaManager(schemaManager);
        service.setSchemaManager(schemaManager);
        schemaManager.loadAllEnabled();

        // Add system partition
        final Partition systemPartition = addPartition(service, "system", ServerDNConstants.SYSTEM_DN);
        service.setSystemPartition(systemPartition);

        // Add default partition from configuration
        final Partition partition = addPartition(service, partitionId, partitionDn);

        // Startup the service
        service.startup();

        // Add base and admin entries
        final CoreSession session = service.getAdminSession();
        if (!session.exists(partition.getSuffixDn())) {
            final DN dn = new DN(partitionDn);
            final ServerEntry rootEntry = service.newEntry(dn);
            rootEntry.add("objectClass", "top", "domain", "extensibleObject");
            rootEntry.add("dc", partitionId);
            session.add(rootEntry);

            final ServerEntry newEntry = service.newEntry(new DN("cn=admin," + dn.toString()));
            newEntry.add("objectClass", "organizationalRole", "simpleSecurityObject");
            newEntry.add("cn", ADMIN_CN);
            newEntry.add("userPassword", ADMIN_PWD.getBytes());
            newEntry.add("description", "Test server admin");
            session.add(newEntry);
        }
        return service;
    }

    private static Partition addPartition(DirectoryService service, String partitionId, String partitionDn) throws Exception {
        final JdbmPartition partition = new JdbmPartition();
        partition.setId(partitionId);
        partition.setPartitionDir(new File(service.getWorkingDirectory(), partitionId));
        partition.setSuffix(partitionDn);
        service.addPartition(partition);
        return partition;
    }

    public static class Builder {

        private int     port;
        private boolean allowAnonymousAccess;
        private boolean enableChangelog;
        private File    workingDir;
        private String  partitionId;
        private String  partitionSuffix;
        private long    maxSizeLimit;

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setWorkingDir(File workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        public Builder useTmpWorkingDir() throws IOException {
            return setWorkingDir(createTempDirectory("ldap-server").toFile());
        }

        public Builder allowAnonymousAccess() {
            this.allowAnonymousAccess = true;
            return this;
        }

        public Builder enableChangelog() {
            this.enableChangelog = true;
            return this;
        }

        public Builder setPartitionId(String partitionId) {
            this.partitionId = partitionId;
            return this;
        }

        public Builder setPartitionDn(String partitionSuffix) {
            this.partitionSuffix = partitionSuffix;
            return this;
        }

        public Builder setMaxSizeLimit(long limit) {
            this.maxSizeLimit = limit;
            return this;
        }

        public MyLdapServer build() throws Exception {
            return new MyLdapServer(workingDir,
                                    partitionSuffix,
                                    partitionId,
                                    port,
                                    enableChangelog,
                                    allowAnonymousAccess,
                                    maxSizeLimit);
        }
    }
}
