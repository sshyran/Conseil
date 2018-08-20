package tech.cryptonomic.conseil

import java.sql.Timestamp

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import tech.cryptonomic.conseil.tezos._
import tech.cryptonomic.conseil.tezos.TezosTypes.{BlockHeader, BlockMetadata}
import tech.cryptonomic.conseil.util.DatabaseUtil

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scala.annotation.tailrec
import scala.compat.Platform

/**
  * Entry point for synchronizing data between the Tezos blockchain and the Conseil database.
  */
object Lorre extends App with LazyLogging {

  private val network =
    if (args.length > 0) args(0)
    else {
      Console.err.println("""
      | No tezos network was provided to connect to
      | Please provide a valid network as an argument to the command line""".stripMargin)
      sys.exit(1)
    }


  private val conf = ConfigFactory.load
  private val awaitTimeInSeconds = conf.getInt("dbAwaitTimeInSeconds")
  private val sleepIntervalInSeconds = conf.getInt("lorre.sleepIntervalInSeconds")
  private val feeUpdateInterval = conf.getInt("lorre.feeUpdateInterval")
  private val purgeAccountsInterval = conf.getInt("lorre.purgeAccountsInterval")

  //re-use the dispatcher pool from the node interface
  implicit val dispatcher = TezosNodeInterface.system.dispatcher

  lazy val db = DatabaseUtil.db
  val tezosNodeOperator = new TezosNodeOperator(TezosNodeInterface)

  @tailrec
  def mainLoop(iteration: Int): Unit = {
    Await.ready(
      processTezosBlocks().flatMap(
        _ =>
          processTezosAccounts()
      )
    , atMost = Duration.Inf)
    if (iteration % feeUpdateInterval == 0) {
      FeeOperations.processTezosAverageFees()
    }
    if (iteration % purgeAccountsInterval == 0) {
      TezosDatabaseOperations.purgeOldAccounts()
    }
    logger.info("Taking a nap")
    Thread.sleep(sleepIntervalInSeconds * 1000)
    mainLoop(iteration + 1)
  }

  logger.info("About to start processing on the {} network", network)
  try {mainLoop(0)} finally db.close()

  /**
    * Fetches all blocks not in the database from the Tezos network and adds them to the database.
    */
  def processTezosBlocks(): Future[Unit] = {
    logger.info("Processing Tezos Blocks...")
/* TODO REMOVE this is only here temporarily for testing purposes
    val top = BlockMetadata(
      protocol = "1",
      chain_id = Some("NetXJwMHeUZC57y"),
      hash = "BLWcWwuvFdzFJXzRQxbwTCFqBnd5E3rCTCaDiUTqaeeboHWgb1i",
      header = TezosTypes.BlockHeader(
        level = 1000,
        proto = 1,
        predecessor = "BLvTH2QaTe2y96wk9ebwAy7aky2hx9p3G9yryDsLZgCJ97GPPnn",
        timestamp = new Timestamp(1532163107000L),
        validationPass = 0,
        operations_hash = Some("LLoa4XzL8hpxwLuxexkEySVNcygp9ysidh3TyGAwSbzGtxzZ3gr9D"),
        fitness = Seq("00,000000000000601f"),
        context = "CoVoUWxsZqHabGeXdBVfBgP8nzuxXqHgiq5GCnHeuAUL5ynWYgtd",
        signature = Some("sigoPBvUD2qGxC2ytyv4omRKkRm6DtN2zcaHYbvdnAhaiVR7RZqd3ZDJXG7toy63zhyqq31fjiqaUyqcWTS5tAxonEPjiSxa")
      )
    )
*/
    val start = System.nanoTime()
    val stored = tezosNodeOperator.getBlocksNotInDatabase(network, followFork = true).flatMap {
        blocks =>
          TezosDatabaseOperations.writeBlocksToDatabase(blocks, db).andThen {
              case Success(_) =>
                val done = System.nanoTime()
                logger.info("Wrote {} blocks to the database in {} seconds.", blocks.size, (done - start).toDouble/1e9)
              case Failure(e) => logger.error(s"Could not write blocks to the database because $e")
            }
      }

    stored.failed.foreach( e => logger.error("Could not fetch blocks from client", e))

    stored
  }

  /**
    * Fetches and stores all accounts from the latest block stored in the database.
    */
  def processTezosAccounts(): Future[Unit] = {
    logger.info("Processing latest Tezos accounts data..")
    tezosNodeOperator.getLatestAccounts(network) transform {
      case Success(accountsInfo) =>
        Try {
          val dbFut = TezosDatabaseOperations.writeAccountsToDatabase(accountsInfo, db)
          dbFut onComplete {
            case Success(_) => logger.info(s"Wrote ${accountsInfo.accounts.size} accounts to the database.")
            case Failure(e) => logger.error(s"Could not write accounts to the database because $e")
          }
          Await.result(dbFut, Duration.Inf)
        }
      case Failure(e) =>
        logger.error("Could not fetch accounts from client", e)
        throw e
    }
  }





}