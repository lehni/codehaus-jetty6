//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.util;

import java.io.IOException;

public class Utf8StringBuffer 
{
    StringBuffer _buffer;
    int _more;
    int _bits;
    
    Utf8StringBuffer()
    {
        _buffer=new StringBuffer();
    }
    
    Utf8StringBuffer(int capacity)
    {
        _buffer=new StringBuffer(capacity);
    }
    
    public void append(byte b)
    {
        if (b>0)
        {
            if (_more>0)
                throw new IllegalStateException();
            _buffer.append((char)(0x7f&b));
        }
        else if (_more==0)
        {
            if ((b&0xc0)!=0xc0)
                // 10xxxxxx
                throw new IllegalStateException();
            else if ((b & 0xe0) == 0xc0)
            {
                //110xxxxx
                _more=1;
                _bits=b&0x1f;
            }
            else if ((b & 0xf0) == 0xe0)
            {
                //1110xxxx
                _more=2;
                _bits=b&0x0f;
            }
            else if ((b & 0xf8) == 0xf0)
            {
                //11110xxx
                _more=3;
                _bits=b&0x07;
            }
            else if ((b & 0xfc) == 0xf8)
            {
                //111110xx
                _more=4;
                _bits=b&0x03;
            }
            else if ((b & 0xfe) == 0xfc) 
            {
                //1111110x
                _more=5;
                _bits=b&0x01;
            }
        }
        else
        {
            if ((b&0xc0)==0xc0)
                // 11??????
                throw new IllegalStateException();
            else
            {
                // 10xxxxxx
                _bits=(_bits<<6)|(b&0x3f);
                if (--_more==0)
                    _buffer.append((char)_bits);
            }
        }
    }
    
    public void clear()
    {
        _buffer.setLength(0);
        _more=0;
        _bits=0;
    }
    
    public String toString()
    {
        if (_more>0)
            throw new IllegalStateException();
        return _buffer.toString();
    }
}