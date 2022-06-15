package server.hibernate.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "links", schema = "public", catalog = "de9oban5pa49j0")
public class LinksEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "link_id")
    private int linkId;
    @Basic
    @Column(name = "link_name")
    private String linkName;
    @Basic
    @Column(name = "file_id")
    private int fileId;
    @Basic
    @Column(name = "users_directory")
    private int usersDirectory;

    public int getLinkId() {
        return linkId;
    }

    public void setLinkId(int linkId) {
        this.linkId = linkId;
    }

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getUsersDirectory() {
        return usersDirectory;
    }

    public void setUsersDirectory(int usersDirectory) {
        this.usersDirectory = usersDirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinksEntity that = (LinksEntity) o;
        return linkId == that.linkId && fileId == that.fileId && usersDirectory == that.usersDirectory && Objects.equals(linkName, that.linkName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkId, linkName, fileId, usersDirectory);
    }
}
