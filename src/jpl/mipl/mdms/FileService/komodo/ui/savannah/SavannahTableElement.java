package jpl.mipl.mdms.FileService.komodo.ui.savannah;

/**
 * <b>Purpose:</b>
 *  Interface for elements of the SavannahTableModel.
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
 * @version $Id: SavannahTableElement.java,v 1.2 2004/12/15 03:47:41 ntt Exp $
 *
 */

public interface SavannahTableElement
{
    public final int  TYPE_FILE    = 0;
    public final int  TYPE_FOLDER  = 1;
    
    public final long SIZE_UNKNOWN = -1L;
    public final long DATE_UNKNOWN = -1L;
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the associated identification of element, depending
     * on what the implementation selects as most meaningful.  For
     * example, local file system entries might returns the fullpath
     * while FEI entries may return merely the filename.
     * @return String representing element
     */
    
    public String toString();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if this object is equal to the other object.
     * @param obj The object with which this is compared.
     * @return True if this is equal to obj, false otherwise.
     */
    
    public boolean equals(Object obj);
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns a hash code value for the object.  
     *  @return Hash value for this object
     */
    
    public int hashCode();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the type of file to differentiate between a normal file
     * and a folder, one of TYPE_FILE or TYPE_FOLDER.
     * @return Type of file
     */
    
    public int getType();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the filesize if known of SIZE_UNKNOWN if size is not
     * assigned a known value.
     * @return File size or SIZE_UNKNOWN.
     */
    
    public long   getSize();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the element file name.
     * @return name of element file.
     */
     
    public String getName();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the absolute path of the file.
     * For local file system, this is the same as File.getAbsolutePath(),
     * for FEI, this might be /filetype or /filetype/filename.
     * @return Full path to entry
     */
    
    public String getPath();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the last modificiation date of entry, or -1 if
     * not defined.
     * @return Modification date or -1 is not defined.
     */
    
    public long getModificationDate();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if element is considered a parent node in the
     * file heirarchy, false otherwise.
     * @return True if element is parent, false otherwise.
     */
    
    public boolean isParent();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns string representation of parent, either a directory,
     * or filetype path.
     * @return Parent path
     */
    
    public String getParent();
    
    //---------------------------------------------------------------------
    
}
