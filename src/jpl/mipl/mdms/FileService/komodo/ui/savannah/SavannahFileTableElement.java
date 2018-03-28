
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.io.File;

/**
 * <b>Purpose:</b>
 *  Implementation of SavannahTableElement for local file system.
 *
 *   <PRE>
 *   Copyright 2004, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2004.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 09/08/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahFileTableElement.java,v 1.2 2004/12/15 03:47:40 ntt Exp $
 *
 */

public class SavannahFileTableElement implements SavannahTableElement
{
    private final String __classname = "SavannahFileTableElement";
    
    protected File    _file;
    protected String  _filepath;
    protected String  _filename;
    protected long    _filesize;
    protected long    _fileModified;
    protected int     _filetype;
    protected boolean _isParent;
    protected String  _parent;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param filepath Path of exising file (file or directory)
     */
    
    public SavannahFileTableElement(String filepath)
    {
        this(filepath, false);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param filepath Path of exising file (file or directory)
     * @param isParent Flag indicating that this element is a parent
     *        in the file hierarchy.
     */
    
    public SavannahFileTableElement(String filepath, boolean isParent)
    {
        this(new File(filepath), isParent);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param file Instance of exising file (file or directory)
     */
    
    public SavannahFileTableElement(File file)
    {
        this(file, false);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param file Instance of exising file (file or directory)
     * @param isParent Flag indicating that this element is a parent
     *        in the file hierarchy.
     */
    
    public SavannahFileTableElement(File file, boolean isParent)
    {
        if (file == null)
            throw new IllegalArgumentException(__classname+
                        "::constructor:: File parameter cannot be null");
        
        this._file         = file;
        this._filepath     = _file.getAbsolutePath();
        this._filename     = _file.getName();
        this._filesize     = _file.length();
        this._fileModified = _file.lastModified();
        this._filetype     = (_file.isDirectory() ? TYPE_FOLDER : TYPE_FILE);
        this._isParent     = isParent;
        this._parent       = _file.getParent();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the associated identification of element, depending
     * on what the implementation selects as most meaningful.  For
     * example, local file system entries might returns the fullpath
     * while FEI entries may return merely the filename.
     * @return String representing element
     */
    
    public String toString()
    {
        return this._filepath;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the type of file to differentiate between a normal file
     * and a folder, one of TYPE_FILE or TYPE_FOLDER.
     * @return Type of file
     */
    
    public int getType()
    {
        return this._filetype;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the filesize if known of SIZE_UNKNOWN if size is not
     * assigned a known value.
     * @return File size or SIZE_UNKNOWN.
     */
    
    public long  getSize()
    {
        return this._filesize;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the element file name.
     * @return name of element file.
     */
     
    public String getName()
    {
        return this._filename;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the absolute path of the file.
     * For local file system, this is the same as File.getAbsolutePath(),
     * for FEI, this might be /filetype or /filetype/filename.
     * @return Full path to entry
     */
    
    public String getPath()
    {
        return this._filepath;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the last modificiation date of entry, or -1 if
     * not defined.
     * @return Modification date or -1 is not defined.
     */
    
    public long getModificationDate()
    {
        return this._fileModified;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns file reference.
     * @return Reference to file object.
     */
    
    public File getFile()
    {
        return this._file;
    }
    
    //---------------------------------------------------------------------
    
    public boolean isParent()
    {
        return this._isParent;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if this object is equal to the other object.
     * @param obj The object with which this is compared.
     * @return True if this is equal to obj, false otherwise.
     */
    
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        
        if (!(obj instanceof SavannahFileTableElement))
            return false;
        
        SavannahFileTableElement other = (SavannahFileTableElement) obj;
        
        return (this._filetype == other.getType() && 
                this._filepath.equals(other.getPath()));
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns a hash code value for the object.  Currently
     *  filepath and filetype are used as part of the
     *  hash value calculation.
     *  @return Hash value for this object
     */
    
    public int hashCode()
    {
        int hash = this._filetype;
        hash = (31 * hash) + this._filepath.hashCode();
        return hash;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns string representation of filetype if file or server group
     * if filetype folder.
     * @return Parent path
     */
    
    public String getParent()
    {
        return this._parent;
    }
    
    //---------------------------------------------------------------------
    
}