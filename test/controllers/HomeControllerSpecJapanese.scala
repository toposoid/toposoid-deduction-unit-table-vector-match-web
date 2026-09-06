/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import org.apache.pekko.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{SentenceType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, PropositionRelation, Reference}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.test.utils.TestUtils
import controllers.TestUtilsEx.{getUUID, registerSingleClaim}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, status, _}
import play.api.test._

import scala.concurrent.duration.DurationInt
import com.ideal.linked.toposoid.common.ActionModeType
import com.ideal.linked.toposoid.protocol.model.base.VerifyingEdges
import com.ideal.linked.toposoid.knowledgebase.regist.model.ImageReference
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForTable
import com.ideal.linked.toposoid.test.utils.TestUtils.{uploadTable, getAnalyzedSentenceObjectsJson}
import com.ideal.linked.toposoid.knowledgebase.regist.model.TableReference
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeForTable


class HomeControllerSpecJapanese extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  val transversalStateJson:String = Json.toJson(transversalState).toString()

  before {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
    //ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_SENTENCE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    ToposoidUtils.callComponent("{}", conf.getString("TOPOSOID_TABLE_VECTORDB_ACCESSOR_HOST"), conf.getString("TOPOSOID_TABLE_VECTORDB_ACCESSOR_PORT"), "createSchema", transversalState)
    Thread.sleep(1000)
  }

  override def beforeAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def afterAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]
  

  val sentence1 = "証拠データが一つあります。"
  val reference1 = Reference(url = "", surface = "証拠データが", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0")
  val tableReference1 = TableReference(reference1, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTable1 = KnowledgeForTable(getUUID(), tableReference1)  

  val paraphrase1 = "証拠サンプルが一つあります。"
  val referencePara1Ok = Reference(url = "", surface = "証拠サンプルが", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0")  
  val tableReferencePara1Ok = TableReference(referencePara1Ok, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTablePara1Ok = KnowledgeForTable(getUUID(), tableReferencePara1Ok)
  val referencePara1Ng = Reference(url = "", surface = "証拠サンプルが", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040389153&fileKind=0")
  val tableReferencePara1Ng = TableReference(referencePara1Ng, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara1Ng = KnowledgeForTable(getUUID(), tableReferencePara1Ng)  


  val sentence2 = "証拠データ1と証拠データ2が1セットあります。"
  val reference2a = Reference(url = "", surface = "証拠データ１と", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0")
  val tableReference2a = TableReference(reference2a, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTable2a = KnowledgeForTable(getUUID(), tableReference2a)  
  val reference2b = Reference(url = "", surface = "証拠データ２が", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReference2b = TableReference(reference2b, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTable2b = KnowledgeForTable(getUUID(), tableReference2b)  


  val paraphrase2 = "証拠サンプル1と証拠サンプル2が1セットあります。"
  val referencePara2aOk = Reference(url = "", surface = "証拠サンプル１と", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0")
  val tableReferencePara2aOk = TableReference(referencePara2aOk, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTablePara2aOk = KnowledgeForTable(getUUID(), tableReferencePara2aOk)  
  val referencePara2bOk = Reference(url = "", surface = "証拠サンプル２が", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReferencePara2bOk = TableReference(referencePara2bOk, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara2bOk = KnowledgeForTable(getUUID(), tableReferencePara2bOk)  

  val referencePara2aNg = Reference(url = "", surface = "証拠サンプル１と", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReferencePara2aNg = TableReference(referencePara2aNg, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")
  val knowledgeForTablePara2aNg = KnowledgeForTable(getUUID(), tableReferencePara2aNg)  
  val referencePara2bNg = Reference(url = "", surface = "証拠サンプル２が", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000032117292&fileKind=0")
  val tableReferencePara2bNg = TableReference(referencePara2bNg, skipHeaderRows=2, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara2bNg = KnowledgeForTable(getUUID(), tableReferencePara2bNg)  


  val sentence3 = "証拠データ1は証拠データ2ではない。"
  val reference3a = Reference(url = "", surface = "証拠データ１は", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0")
  val tableReference3a = TableReference(reference3a, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTable3a = KnowledgeForTable(getUUID(), tableReference3a)  
  val reference3b = Reference(url = "", surface = "証拠データ２ではない。", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReference3b = TableReference(reference3b, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTable3b = KnowledgeForTable(getUUID(), tableReference3b)  

  val paraphrase3 = "証拠サンプル1は証拠サンプル2ではない。"
  val referencePara3aOk = Reference(url = "", surface = "証拠サンプル１は", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0")
  val tableReferencePara3aOk = TableReference(referencePara3aOk, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTablePara3aOk = KnowledgeForTable(getUUID(), tableReferencePara3aOk)  
  val referencePara3bOk = Reference(url = "", surface = "証拠サンプル２ではない。", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReferencePara3bOk = TableReference(referencePara3bOk, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara3bOk = KnowledgeForTable(getUUID(), tableReferencePara3bOk)  


  val paraphrase4 = "証拠サンプル1は証拠サンプル2である。"
  val referencePara4aOk = Reference(url = "", surface = "証拠サンプル１は", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000001086170&fileKind=0")
  val tableReferencePara4aOk = TableReference(referencePara4aOk, skipHeaderRows=5, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "se0101")
  val knowledgeForTablePara4aOk = KnowledgeForTable(getUUID(), tableReferencePara4aOk)  
  val referencePara4bOk = Reference(url = "", surface = "証拠サンプル２である。", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReferencePara4bOk = TableReference(referencePara4bOk, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara4bOk = KnowledgeForTable(getUUID(), tableReferencePara4bOk)  



  /*
  val referencePara2aNg = Reference(url = "", surface = "証拠サンプル１と", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReferencePara2aNg = TableReference(referencePara2aNg, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")
  val knowledgeForTablePara2aNg = KnowledgeForTable(getUUID(), tableReferencePara2aNg)  
  val referencePara2bNg = Reference(url = "", surface = "証拠サンプル２が", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000032117292&fileKind=0")
  val tableReferencePara2bNg = TableReference(referencePara2bNg, skipHeaderRows=2, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara2bNg = KnowledgeForTable(getUUID(), tableReferencePara2bNg)  
  */


  /*
  val sentence2 = "証拠データを一つ提出します。"
  val reference2 = Reference(url = "", surface = "証拠データを", surfaceIndex = -1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040292480&fileKind=1")
  val tableReference2 = TableReference(reference2, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")  
  val knowledgeForTable2 = KnowledgeForTable(getUUID(), tableReference2)    
  
  val sentence3 = "証拠データが一つ必要です。"
  val reference3 = Reference(url = "", surface = "証拠データが", surfaceIndex = -1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReference3 = TableReference(reference3, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")  
  val knowledgeForTable3 = KnowledgeForTable(getUUID(), tableReference3)  

  val sentence4 = "立証用の証拠データに依存します。"  
  val reference4 = Reference(url = "", surface = "証拠データに", surfaceIndex = -1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000032117292&fileKind=0")
  val tableReference4 = TableReference(reference4, skipHeaderRows=2, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")  
  val knowledgeForTable4 = KnowledgeForTable(getUUID(), tableReference4)  


  val paraphrase2 = "証拠サンプルを一つ提出します。"
  val referencePara2Ok = Reference(url = "", surface = "", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040292480&fileKind=1")
  val tableReferencePara2Ok = TableReference(referencePara2Ok, skipHeaderRows=8, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara2Ok = KnowledgeForTable(getUUID(), tableReferencePara2Ok)  
  val referencePara2Ng = Reference(url = "", surface = "", surfaceIndex = -1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000031927775&fileKind=0")
  val tableReferencePara2Ng = TableReference(referencePara2Ng, skipHeaderRows=1, skipRowList=List(),multiHeaderRows=2, sheetNameForExcel= "表4")
  val knowledgeForTablePara2Ng = KnowledgeForTable(getUUID(), tableReferencePara2Ng)    

  val paraphrase3 = "証拠サンプルが一つ必要です。"
  val referencePara3Ok = Reference(url = "", surface = "", surfaceIndex = -1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040410921&fileKind=4")
  val tableReferencePara3Ok = TableReference(referencePara3Ok, skipHeaderRows=3, skipRowList=List(),multiHeaderRows=3, sheetNameForExcel= "")
  val knowledgeForTablePara3Ok = KnowledgeForTable(getUUID(), tableReferencePara3Ok)  
  val referencePara3Ng = Reference(url = "", surface = "", surfaceIndex = -1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040482933&fileKind=1")
  val tableReferencePara3Ng = TableReference(referencePara3Ng, skipHeaderRows=1, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara3Ng = KnowledgeForTable(getUUID(), tableReferencePara3Ng)    


  val paraphrase4 = "立証用の証拠サンプルに依存します。"
  val referencePara4Ok = Reference(url = "", surface = "", surfaceIndex = -1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000032117292&fileKind=0")
  val tableReferencePara4Ok = TableReference(referencePara4Ok, skipHeaderRows=2, skipRowList=List(),multiHeaderRows=1, sheetNameForExcel= "")
  val knowledgeForTablePara4Ok = KnowledgeForTable(getUUID(), tableReferencePara4Ok)    
  val referencePara4Ng = Reference(url = "", surface = "", surfaceIndex = -1, isWholeSentence = false,
    originalUrlOrReference = "https://www.e-stat.go.jp/stat-search/file-download?statInfId=000040491301&fileKind=1")
  val tableReferencePara4Ng = TableReference(referencePara4Ng, skipHeaderRows=0, skipRowList=List(),multiHeaderRows=4, sheetNameForExcel= "")
  val knowledgeForTablePara4Ng = KnowledgeForTable(getUUID(), tableReferencePara4Ng)        
  */
  /*
  val sentence1 = "猫が２匹います。"
  val reference1 = Reference(url = "", surface = "猫が", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/val2017/000000039769.jpg")
  val imageReference1 = ImageReference(reference1, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForTable1 = knowledgeForTable(getUUID(), imageReference1)      
  //val imageBoxInfo1 = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)

  val paraphrase1 = "ペットが２匹います。"
  val referencePara1Ok = Reference(url = "", surface = "ペットが", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/val2017/000000039769.jpg")
  val imageReferencePara1Ok = ImageReference(referencePara1Ok, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForTablePara1Ok = knowledgeForTable(getUUID(), imageReferencePara1Ok)  
  //val imageBoxInfoPara1Ok = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)

  val referencePara1Ng = Reference(url = "", surface = "ペットが", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/train2017/000000428746.jpg")
  val imageReferencePara1Ng = ImageReference(referencePara1Ng, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForTablePara1Ng = knowledgeForTable(getUUID(), imageReferencePara1Ng)    
  //val imageBoxInfoPara1Ng = ImageBoxInfo(x = 77, y = 98, weight = 433, height = 222)
  */

  /*
  val sentence2 = "猫と犬がいます。"
  val reference2a = Reference(url = "", surface = "猫と", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/val2017/000000039769.jpg")
  val imageReference2a = ImageReference(reference2a, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForTable2a = knowledgeForTable(getUUID(), imageReference2a)      
  //val imageBoxInfo2a = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)
  
  val reference2b = Reference(url = "", surface = "犬が", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/train2017/000000428746.jpg")
  val imageReference2b = ImageReference(reference2b, x = 77, y = 98, width = 433, height = 222)  
  val knowledgeForTable2b = knowledgeForTable(getUUID(), imageReference2b)       
  //val imageBoxInfo2b = ImageBoxInfo(x = 77, y = 98, weight = 433, height = 222)

  val paraphrase2 = "ペットと動物がいます。"
  val referencePara2aOk = Reference(url = "", surface = "ペットと", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/val2017/000000039769.jpg")
  val imageReferencePara2aOk = ImageReference(referencePara2aOk, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForTablePara2aOk = knowledgeForTable(getUUID(), imageReferencePara2aOk)  
  //val imageBoxInfoPara2aOk = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)
  
  val referencePara2bOk = Reference(url = "", surface = "動物が", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/train2017/000000428746.jpg")
  val imageReference2bOk = ImageReference(referencePara2bOk, x = 77, y = 98, width = 433, height = 222)  
  val knowledgeForTablePara2bOk = knowledgeForTable(getUUID(), imageReference2bOk)       
  //val imageBoxInfoPara2bOk = ImageBoxInfo(x = 77, y = 98, weight = 433, height = 222)
  
  val referencePara2aNg = Reference(url = "", surface = "ペットと", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "https://farm8.staticflickr.com/7287/8737869589_16ab5a83c4_z.jpg")
  val imageReferencePara2aNg = ImageReference(referencePara2aNg, x = 0, y = 0, width = 630, height = 420)
  val knowledgeForTablePara2aNg = knowledgeForTable(getUUID(), imageReferencePara2aNg)      
  //val imageBoxInfoPara2aNg = ImageBoxInfo(x = 0, y = 0, weight = 630, height = 420)
  val referencePara2bNg = Reference(url = "", surface = "動物が", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "https://farm8.staticflickr.com/7287/8737869589_16ab5a83c4_z.jpg")
  val imageReferencePara2bNg = ImageReference(referencePara2bNg, x = 0, y = 0, width = 630, height = 420)
  val knowledgeForTablePara2bNg = knowledgeForTable(getUUID(), imageReferencePara2bNg)      
  //val imageBoxInfoPara2bNg = ImageBoxInfo(x =0 , y = 0, weight = 630, height = 420)

  val sentence3 = "猫は犬ではない。"
  val reference3a = Reference(url = "", surface = "猫は", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/val2017/000000039769.jpg")
  val imageReference3a = ImageReference(reference3a, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForTable3a = knowledgeForTable(getUUID(), imageReference3a)  

  //val imageBoxInfo3a = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)
  val reference3b = Reference(url = "", surface = "犬ではない。", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/train2017/000000428746.jpg")
  val imageReference3b = ImageReference(reference3b, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForTable3b = knowledgeForTable(getUUID(), imageReference3b)    
  //val imageBoxInfo3b = ImageBoxInfo(x = 77, y = 98, weight = 433, height = 222)

  val paraphrase3 = "ペットは友達でない。"
  val referencePara3aOk = Reference(url = "", surface = "ペットは", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/val2017/000000039769.jpg")
  val imageReferencePara3aOk = ImageReference(referencePara3aOk, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForTablePara3aOk = knowledgeForTable(getUUID(), imageReferencePara3aOk)    
  //val imageBoxInfoPara3aOk = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)

  val referencePara3bOk = Reference(url = "", surface = "友達でない。", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/train2017/000000428746.jpg")
  val imageReferencePara3bOk = ImageReference(referencePara3bOk, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForTablePara3bOk = knowledgeForTable(getUUID(), imageReferencePara3bOk)    
  //val imageBoxInfoPara3bOk = ImageBoxInfo(x = 77, y = 98, weight = 433, height = 222)

  val paraphrase4 = "ペットは友達である。"
  val referencePara4aOk = Reference(url = "", surface = "ペットは", surfaceIndex = 0, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/val2017/000000039769.jpg")
  val imageReferencePara4aOk = ImageReference(referencePara4aOk, x = 11, y = 11, width = 466, height = 310)
  val knowledgeForTablePara4aOk = knowledgeForTable(getUUID(), imageReferencePara4aOk)    
  //val imageBoxInfoPara3aOk = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)
  
  //val imageBoxInfoPara4aOk = ImageBoxInfo(x =11 , y = 11, weight = 466, height = 310)
  val referencePara4bOk = Reference(url = "", surface = "友達である。", surfaceIndex = 1, isWholeSentence = false,
    originalUrlOrReference = "http://images.cocodataset.org/train2017/000000428746.jpg")
  val imageReferencePara4bOk = ImageReference(referencePara4bOk, x = 77, y = 98, width = 433, height = 222)
  val knowledgeForTablePara4bOk = knowledgeForTable(getUUID(), imageReferencePara4bOk)    
  //val imageBoxInfoPara4bOk = ImageBoxInfo(x = 77, y = 98, weight = 433, height = 222)
  */
  
  val lang = "ja_JP"
  //片側対象、片側一致
  "The specification1" should {
    "returns an appropriate response" in {
      val propositionId1 = getUUID()
      val sentenceId1 = getUUID()
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false, List(imageA))
      val knowledge1 = Knowledge(lang=lang, sentence=sentence1, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTable1, transversalState)))
      val paraphraseKnowledge1 = Knowledge(lang=lang, sentence=paraphrase1, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTablePara1Ok, transversalState)))
      registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = getUUID()
      val sentenceIdForInference1 = getUUID()
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentenceForParser = InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)
      //val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze")
      val json = getAnalyzedSentenceObjectsJson(lang, inputSentenceForParser, transversalState)
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 1)

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
    }
  }
  
  //片側対象、片側不一致
  "The specification2" should {
    "returns an appropriate response" in {
      val propositionId1 = getUUID()
      val sentenceId1 = getUUID()
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false, List(imageA))
      val knowledge1 = Knowledge(lang=lang, sentence=sentence1, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTable1, transversalState)))
      val paraphraseKnowledge1 = Knowledge(lang=lang, sentence=paraphrase1, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTablePara1Ng, transversalState)))
      registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = getUUID()
      val sentenceIdForInference1 = getUUID()
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentenceForParser = InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)
      //val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze")
      val json = getAnalyzedSentenceObjectsJson(lang, inputSentenceForParser, transversalState)
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 1)

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
    }
  }
  
  //両側対象、両側一致
  "The specification3" should {
    "returns an appropriate response" in {
      val propositionId1 = getUUID()
      val sentenceId1 = getUUID()
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false, List(imageA))
      val knowledge1 = Knowledge(lang=lang, sentence=sentence2, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTable2a, transversalState), uploadTable(knowledgeForTable2b, transversalState)))
      val paraphraseKnowledge1 = Knowledge(lang=lang, sentence=paraphrase2, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTablePara2aOk, transversalState), uploadTable(knowledgeForTablePara2bOk, transversalState)))
      registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      
      val propositionIdForInference1 = getUUID()
      val sentenceIdForInference1 = getUUID()
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentenceForParser = InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)
      //val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze")
      val json = getAnalyzedSentenceObjectsJson(lang, inputSentenceForParser, transversalState)
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 2)

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      
    }
  }
  
  //両側対象、片側のみ一致その１
  "The specification4a" should {
    "returns an appropriate response" in {
      val propositionId1 = getUUID()
      val sentenceId1 = getUUID()
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false, List(imageA))
      val knowledge1 = Knowledge(lang=lang, sentence=sentence2, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTable2a, transversalState), uploadTable(knowledgeForTable2b, transversalState)))
      val paraphraseKnowledge1 = Knowledge(lang=lang, sentence=paraphrase2, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTablePara2aNg, transversalState), uploadTable(knowledgeForTablePara2bOk, transversalState)))
      registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      
      val propositionIdForInference1 = getUUID()
      val sentenceIdForInference1 = getUUID()
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentenceForParser = InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)
      //val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze")
      val json = getAnalyzedSentenceObjectsJson(lang, inputSentenceForParser, transversalState)
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 2)

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      
    }
  }
  
  //両側対象、片側のみ一致その2
  "The specification4b" should {
    "returns an appropriate response" in {
      val propositionId1 = getUUID()
      val sentenceId1 = getUUID()
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false, List(imageA))
      val knowledge1 = Knowledge(lang=lang, sentence=sentence2, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTable2a, transversalState), uploadTable(knowledgeForTable2b, transversalState)))
      val paraphraseKnowledge1 = Knowledge(lang=lang, sentence=paraphrase2, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTablePara2aOk, transversalState), uploadTable(knowledgeForTablePara2bNg, transversalState)))
      registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      
      val propositionIdForInference1 = getUUID()
      val sentenceIdForInference1 = getUUID()
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentenceForParser = InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)
      //val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze")
      val json = getAnalyzedSentenceObjectsJson(lang, inputSentenceForParser, transversalState)
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 2)

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      
    }
  }
  
  //両側対象、否定一致
  "The specification5" should {
    "returns an appropriate response" in {
      val propositionId1 = getUUID()
      val sentenceId1 = getUUID()
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false, List(imageA))
      val knowledge1 = Knowledge(lang=lang, sentence=sentence3, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTable3a, transversalState), uploadTable(knowledgeForTable3b, transversalState)))
      val paraphraseKnowledge1 = Knowledge(lang=lang, sentence=paraphrase3, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTablePara3aOk, transversalState), uploadTable(knowledgeForTablePara3bOk, transversalState)))
      registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      
      val propositionIdForInference1 = getUUID()
      val sentenceIdForInference1 = getUUID()
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentenceForParser = InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)
      //val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze")
      val json = getAnalyzedSentenceObjectsJson(lang, inputSentenceForParser, transversalState)
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 1)

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      
    }
  }
  
  //両側対象、否定不一致
  "The specification6" should {
    "returns an appropriate response" in {
      val propositionId1 = getUUID()
      val sentenceId1 = getUUID()
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false, List(imageA))
      val knowledge1 = Knowledge(lang=lang, sentence=sentence3, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTable3a, transversalState), uploadTable(knowledgeForTable3b, transversalState)))
      val paraphraseKnowledge1 = Knowledge(lang=lang, sentence=paraphrase4, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTablePara4aOk, transversalState), uploadTable(knowledgeForTablePara4bOk, transversalState)))
      registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      
      val propositionIdForInference1 = getUUID()
      val sentenceIdForInference1 = getUUID()
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentenceForParser = InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)
      //val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze")
      val json = getAnalyzedSentenceObjectsJson(lang, inputSentenceForParser, transversalState)
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 0)

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      
    }
  }  
  
  //全て被覆できないケース
  "The specification7" should {
    "returns an appropriate response" in {
      val propositionId1 = getUUID()
      val sentenceId1 = getUUID()
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false, List(imageA))
      val knowledge1 = Knowledge(lang=lang, sentence=sentence2, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTable2a, transversalState), uploadTable(knowledgeForTable2b, transversalState)))
      val paraphraseKnowledge1 = Knowledge(lang=lang, sentence=paraphrase2, extentInfoJson = "{}", knowledgeForTables=List(uploadTable(knowledgeForTablePara2aNg, transversalState), uploadTable(knowledgeForTablePara2bNg, transversalState)))
      registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      
      val propositionIdForInference1 = getUUID()
      val sentenceIdForInference1 = getUUID()
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentenceForParser = InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)
      //val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze")
      val json = getAnalyzedSentenceObjectsJson(lang, inputSentenceForParser, transversalState)
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 2)

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)  
      
    }
  }
  
}
