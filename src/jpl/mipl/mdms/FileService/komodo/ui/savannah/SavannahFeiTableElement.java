
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

/**
 * <b>Purpose:</b>
 *  Implementation of SavannahTableElement for FEI file system.
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
 * @version $Id: SavannahFeiTableElement.java,v 1.2 2004/12/15 03:47:38 ntt Exp $
 *
 */

public class SavannahFeiTableElement implements SavannahTableElement
{
    private final String __classname = "SavannahFeiTableElement";
    
    protected String  _serverGroup;
    protected String  _feiType;
    protected String  _filename;
    protected String  _parent;
    protected String  _filepath;
    protected long    _filesize;
    protected long    _fileModified;
    protected int     _filetype;
    protected String  _comment;
    protected boolean _isParent;
    
    //---------------------------------------------------------------------
    
    public SavannahFeiTableElement(String group,    String feiType,
                                   String filename, long filesize,
                                   long modDate,    String comment,
                                   boolean isParent)
    {
        if (group == null || group.equals(""))
            throw new IllegalArgumentException(__classname+
                "::constructor:: Server group parameter cannot be "+
                "null or empty");
        
        if (feiType == null || feiType.equals(""))
            throw new IllegalArgumentException(__classname+
                "::constructor:: FEI Filetype parameter cannot be "+
                "null or empty");
        
        this._serverGroup  = group;
        this._feiType      = feiType;
        this._filesize     = filesize;
        this._fileModified = modDate;
        this._filepath     = "/" + _serverGroup + "/" + _feiType + "/";
        this._comment      = comment;
        this._isParent     = isParent;
        this._parent       = this._filepath;
        
        //if only a filetype entry
        if (filename == null || filename.equals(""))
        {
            this._filetype = TYPE_FOLDER;
            this._filename = _feiType;
        }
        else //else a file entry in filetype
        {
            this._filetype = TYPE_FILE;
            this._filename = filename;
            this._filepath = _filepath + _filename;
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the filename of the element which is either the
     * FEI type or the filename itself depending on type.
     * @return String representing element
     */
    
    public String toString()
    {
        return this._filename;
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
     * Returns the comment string associated with this element.
     * @return comment string or null if none
     */
    
    public String getComment()
    {
        return this._comment;
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
        
        if (!(obj instanceof SavannahFeiTableElement))
            return false;
        
        SavannahFeiTableElement other = (SavannahFeiTableElement) obj;
        
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
     * Returns the Fei filetype name
     * @return String of filetype
     */
    
    public String getFeiType()
    {
        return this._feiType;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the server group name
     * @return String of server group
     */
    
    public String getFeiGroup()
    {
        return this._serverGroup;
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