package server.hibernate.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "directories", schema = "public", catalog = "de9oban5pa49j0")
public class DirectoriesEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "dir_id")
    private int dirId;
    @Basic
    @Column(name = "user_id")
    private int userId;
    @Basic
    @Column(name = "dir_name")
    private String dirName;
    @Basic
    @Column(name = "parent_dir")
    private Integer parentDir;

    public DirectoriesEntity() {
    }

    public DirectoriesEntity(int userId, String dirName) {
        this.userId = userId;
        this.dirName = dirName;
    }

    public int getDirId() {
        return dirId;
    }

    public void setDirId(int dirId) {
        this.dirId = dirId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public Integer getParentDir() {
        return parentDir;
    }

    public void setParentDir(Integer parentDir) {
        this.parentDir = parentDir;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DirectoriesEntity that = (DirectoriesEntity) o;
        return dirId == that.dirId && userId == that.userId && Objects.equals(dirName, that.dirName) && Objects.equals(parentDir, that.parentDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dirId, userId, dirName, parentDir);
    }
}
