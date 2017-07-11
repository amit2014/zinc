package sbt.inc.binary

import java.nio.file.Paths

import org.scalacheck.{ Prop, Properties }
import sbt.internal.inc.{ Analysis, AnalysisGenerators, FileAnalysisStore }
import sbt.io.IO
import xsbti.compile.analysis.ReadWriteMappers

object BinaryMappersSpecification
    extends Properties("BinaryMappers")
    with BinaryAnalysisFormatSpecification {

  override def RootFilePath = "/tmp/localProject"
  private final val mappers: ReadWriteMappers =
    ReadWriteMappers.getMachineIndependentMappers(Paths.get(RootFilePath))

  object RelativeAnalysisGenerators extends AnalysisGenerators {
    override def RootFilePath = BinaryMappersSpecification.RootFilePath
  }

  private final val ReadFeedback = "The analysis file cannot be read."
  override protected def checkAnalysis(analysis: Analysis): Prop = {
    // Note: we test writing to the file directly to reuse `FileBasedStore` as it is written
    val (readAnalysis0, readSetup) = IO.withTemporaryFile("analysis", "test") { tempAnalysisFile =>
      val fileBasedStore = FileAnalysisStore.binary(tempAnalysisFile, mappers)
      fileBasedStore.set(analysis, commonSetup)
      fileBasedStore.get().getOrElse(sys.error(ReadFeedback))
    }

    val readAnalysis = readAnalysis0 match { case a: Analysis => a }
    compare(analysis, readAnalysis) && compare(commonSetup, readSetup)
  }

  property("The default relative mapper works in empty analysis files") = {
    checkAnalysis(Analysis.empty)
  }

  property("The default relative mapper works in complex analysis files") = {
    import org.scalacheck.Prop.forAllNoShrink
    forAllNoShrink(RelativeAnalysisGenerators.genAnalysis)(checkAnalysis)
  }
}
