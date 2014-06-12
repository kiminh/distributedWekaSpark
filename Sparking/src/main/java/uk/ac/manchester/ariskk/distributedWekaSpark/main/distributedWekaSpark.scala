/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    distributedWekaSpark.scala
 *    Copyright (C) 2014 Koliopoulos Kyriakos-Aris
 *
 */


package uk.ac.manchester.ariskk.distributedWekaSpark.main

import java.util.ArrayList
import weka.core.Utils
import uk.ac.manchester.ariskk.distributedWekaSpark.headers.CSVToArffHeaderSparkJob
import uk.ac.manchester.ariskk.distributedWekaSpark.classifiers.WekaClassifierSparkJob
import uk.ac.manchester.ariskk.distributedWekaSpark.classifiers.WekaClassifierEvaluationSparkJob
import uk.ac.manchester.ariskk.distributedWekaSpark.classifiers.WekaClassifierFoldBasedSparkJob
import java.io.DataOutput
import org.apache.hadoop.io.DataOutputBuffer
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import weka.associations.AssociationRule
import weka.associations.AssociationRulesProducer
import weka.associations.Apriori
import org.apache.spark.rdd.RDD
import weka.core.Instance
import uk.ac.manchester.ariskk.distributedWekaSpark.associationRules.WekaAssociationRulesSparkJob
import uk.ac.manchester.ariskk.distributedWekaSpark.associationRules.UpdatableRule
import java.util.Collections
import java.util.Comparator
import scala.util.Sorting
import weka.core.Instances



/** Project main  
 *  
 *   @author Aris-Kyriakos Koliopoulos (ak.koliopoulos {[at]} gmail {[dot]} com)
 *   
 *   ToDo: user-interface, option parser and loader-saver for persistence  */


object distributedWekaSpark {
   def main(args : Array[String]){
       
     val optionsHandler=new OptionsParser(args.mkString(" "))
       
      //Configuration of Context - need to check that at a large scale: spark seems to add a context by default
      val conf=new SparkConf().setAppName("distributedWekaSpark").setMaster(optionsHandler.getMaster).set("spark.executor.memory","1g")
      val sc=new SparkContext(conf)
      val hdfshandler=new HDFSHandler(sc)
      
      val task=new TaskConfiguration(optionsHandler.getTask,optionsHandler)
      
     
     
     
     
     
      val teststring="asdasdadkjsljflksdjflk;sjfslkafjslkfjskljfal;fjskljflkjfslka;jfasfjkldsjfkjoiucjndhjf"
        
      hdfshandler.saveObjectToHDFS(teststring, "hdfs://sandbox.hortonworks.com:8020/user/weka/", null)
      
    //  exit(0)
     
     
      
      
     
      ///Input Parameters . ToDo: accept params as args(0), args(1) etc from command line , 
      val master=optionsHandler.getMaster
      val hdfsPath=optionsHandler.getHdfsPath
      val numberOfPartitions=optionsHandler.getNumberOfPartitions
      val numberOfAttributes=optionsHandler.getNumberOfAttributes
      val classifierToTrain=optionsHandler.getClassifier //this must done in-Weka somehow
      val metaL="default"  //default is weka.classifiers.meta.Vote
      val classAtt=optionsHandler.getClassIndex
      val randomChunks=optionsHandler.getNumberOfRandomChunks
      var names=new ArrayList[String]
      val folds=optionsHandler.getNumFolds
      val headerJobOptions=Utils.splitOptions("-N first-last")
      val namesPath=optionsHandler.getNamesPath
      
      
      
     
     
      
      
     // System.exit(0)
      
      //Load Dataset and cache. ToDo: global caching strategy   -data.persist(StorageLevel.MEMORY_AND_DISK)
       var dataset=hdfshandler.loadRDDFromHDFS(hdfsPath, numberOfPartitions)
       dataset.cache()
       //glom? here on not?
       val namesfromfile=hdfshandler.loadRDDFromHDFS(namesPath,1)
       println(namesfromfile.collect.mkString(""))
       
 
       names=optionsHandler.getNamesFromString(namesfromfile.collect.mkString(""))
       //headers
        val headerjob=new CSVToArffHeaderSparkJob
        val headers=headerjob.buildHeaders(headerJobOptions,names,numberOfAttributes,dataset)
      // hdfshandler.saveToHDFS(headers, "user/weka/testhdfs.txt", "testtext")
         hdfshandler.saveObjectToHDFS(headers, "hdfs://sandbox.hortonworks.com:8020/user/weka/", null)
         val h=hdfshandler.loadObjectFromHDFS("hdfs://sandbox.hortonworks.com:8020/user/weka/")
        // val h2=new Instances(h)
         exit(0)
        // System.exit(0)
       //randomize if necessary 
      // if(randomChunks>0){dataset=new WekaRandomizedChunksSparkJob().randomize(dataset, randomChunks, headers, classAtt)}
       
     //build foldbased
//      val foldjob=new WekaClassifierFoldBasedSparkJob
//      val classifier=foldjob.buildFoldBasedModel(dataset, headers, folds, classifierToTrain, metaL,classAtt)
//      println(classifier.toString())
//      val evalfoldjob=new WekaClassifierEvaluationSparkJob
//      val eval=evalfoldjob.evaluateFoldBasedClassifier(folds, classifier, headers, dataset,classAtt)
//      evalfoldjob.displayEval(eval)
//      
//      //build a classifier+ evaluate
//      val classifierjob=new WekaClassifierSparkJob
//      val classifier2=classifierjob.buildClassifier(metaL,classifierToTrain,classAtt,headers,dataset,null,optionsHandler.getWekaOptions) 
//      val evaluationJob=new WekaClassifierEvaluationSparkJob
//      val eval2=evaluationJob.evaluateClassifier(classifier2, headers, dataset,classAtt)
//
//      println(classifier2.toString())
//      evaluationJob.displayEval(eval2)
    
      //val broad=sc.broadcast(headers)
      
      val rulejob=new WekaAssociationRulesSparkJob
      val rules=rulejob.findAssociationRules(headers, dataset, 0.1, 1, 1)
      
      val array=new Array[UpdatableRule](rules.keys.size)
      var j=0
      rules.foreach{
        
        keyv => 
//          if(keyv._1=="[att83=t, att32=t, att18=t, att217=high] [att13=t]")println(keyv._2.getRuleString)
//          if(keyv._1=="[att83=t, att14=t, att18=t, att217=high] [att13=t]")println(keyv._2.getRuleString)
//          if(keyv._1=="[att83=t, att14=t, att32=t, att217=high] [att13=t]")println(keyv._2.getRuleString)
       //   if(keyv._2.getSupportCount>100) {//println(keyv._2.getRuleString)}
          array(j)=keyv._2
        j+=1
       }
       Sorting.quickSort(array)
       val fullsupport=new Array[String](array.length)
       val lesssupport=new Array[String](array.length)
       var i=0;var o=0;
       array.foreach{x =>
         x.getTransactions match{
           case  n if n>2500 => fullsupport(i)=x.getRuleString;i+=1
           case _ =>   lesssupport(o)=x.getRuleString;o+=1
         }}
        println("\n Full Support \n")
        fullsupport.foreach{x => if(x!=null)println(x)} 
        println("\n Less support \n")
        lesssupport.foreach{x => if(x!=null)println(x)}
   }
   
     
}