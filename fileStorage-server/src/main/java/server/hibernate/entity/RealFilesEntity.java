package server.hibernate.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "realFiles", schema = "public", catalog = "de9oban5pa49j0")
public class RealFilesEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "file_id")
    private int fileId;
    @Basic
    @Column(name = "directory_id")
    private int directoryId;
    @Basic
    @Column(name = "name")
    private String name;

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(int directoryId) {
        this.directoryId = directoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RealFilesEntity that = (RealFilesEntity) o;
        return fileId == that.fileId && directoryId == that.directoryId && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, directoryId, name);
    }
}
