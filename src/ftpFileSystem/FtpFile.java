package ftpFileSystem;

/**
 * Created by xulingo on 16/3/22.
 */
public class FtpFile {


    String filename;
    String path;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    @Override
    public String toString() {
        return "FtpFile{" +
                "filename='" + filename + '\'' +
                ", path='" + path + '\'' +
                '}';
    }


}
