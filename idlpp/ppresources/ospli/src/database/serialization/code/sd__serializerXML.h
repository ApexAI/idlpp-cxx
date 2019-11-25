/*
 *                         OpenSplice DDS
 *
 *   This software and documentation are Copyright 2006 to 2015 PrismTech
 *   Limited and its licensees. All rights reserved. See file:
 *
 *                     $OSPL_HOME/LICENSE 
 *
 *   for full copyright notice and license terms. 
 *
 */
/** \file services/serialization/include/sd__serializerXML.h
 *  \brief Protected method of the XML serializer class, to be
 *         used only by the class and its descendants.
 */

#ifndef SD__SERIALIZERXML_H
#define SD__SERIALIZERXML_H

#include "sd_serializerXML.h"
#include "sd__deepwalkMeta.h" /* for sd_errorInfo */

c_ulong           sd_XMLSerType(
                      c_type type,
                      c_object object,
                      c_char *dataPtr) __nonnull_all__;

c_bool            sd_XMLDeserType(
                      c_type type,
                      c_object *objectPtr,
                      c_char **dataPtrPtr,
                      sd_errorInfo *errorInfo) __nonnull_all__ __attribute_warn_unused_result__;
                      
c_ulong           sd_printCharData(
                      c_string dst,
                      c_string src) __nonnull_all__;
                      
c_bool            sd_scanCharData(
                      c_char **dst,
                      c_char **src,
                      sd_errorInfo *errorInfo) __nonnull_all__ __attribute_warn_unused_result__;
                      
c_char *          sd_peekTaggedCharData(
                      c_char *src,
                      c_char *tagName) __nonnull_all__;
                      
c_char *          sd_serializerXMLToString(
                      sd_serializer serializer,
                      sd_serializedData serData) __nonnull_all__ __attribute_warn_unused_result__;

sd_serializedData sd_serializerXMLFromString(
                      sd_serializer serializer,
                      const c_char *str) __nonnull_all__ __attribute_warn_unused_result__;
                      
                      
c_bool            sd_XMLDeserCallbackPre(
                      const c_char *name,
                      c_type type,
                      c_object *objectPtr,
                      void *actionArg,
                      sd_errorInfo *errorInfo,
                      void *userData) __nonnull((1, 2, 4, 5)) __attribute_warn_unused_result__;
                      
c_bool            sd_XMLDeserCallbackPost(
                      const c_char *name,
                      c_type type,
                      c_object *objectPtr,
                      void *actionArg,
                      sd_errorInfo *errorInfo,
                      void *userData) __nonnull((1, 2, 3, 4, 5)) __attribute_warn_unused_result__;
                      
                      

#endif /* SD__SERIALIZERXML_H */
