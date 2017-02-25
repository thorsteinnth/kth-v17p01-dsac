
class ReadImposeWriteConsultMajority(init: Init[ReadImposeWriteConsultMajority]) extends ComponentDefinition {

  //subscriptions

  val nnar = provides[AtomicRegister];

  val pLink = requires[PerfectLink];
  val beb = requires[BestEffortBroadcast];

  //state and initialization

  val (self: Address, n: Int, selfRank: Int) = init match {
    case Init(selfAddr: Address, n: Int) => (selfAddr, n, AddressUtils.toRank(selfAddr))
  };

  var (ts, wr) = (0, 0);    // (timestamp, rank/pid)
  var value: Option[Any] = None;
  var acks = 0;
  var readval: Option[Any] = None;
  var writeval: Option[Any] = None;
  var rid = 0;
  var readlist: Map[Address, (Int, Int, Option[Any])] = Map.empty
  var reading = false;

  //handlers

  nnar uponEvent {
    case AR_Read_Request() => handle {
      rid = rid + 1;
      
        /* MY CODE HERE  */
        // Got read request (from client)
        // Broadcast read request to all (start query phase)
        acks = 0;
        readlist.clear;
        reading = true;
        trigger(BEB_Broadcast(READ(rid)) -> beb); // Query request
        
        println(s"$self - GOT READ REQUEST FROM CLIENT - BROADCASTED READ($rid) (QUERY REQUEST)");
        
    };
    case AR_Write_Request(wval) => handle { 
      rid = rid + 1;
         
        /* MY CODE HERE  */
        // Got a write request (from client)
        // Before writing, read from majority to get latest ts
        // (query phase to get the latest ts before the update phase)
        writeval = Some(wval);
        acks = 0;
        readlist.clear;
        trigger(BEB_Broadcast(READ(rid)) -> beb);   // Query request
     
        println(s"$self - GOT WRITE REQUEST FOR $wval FROM CLIENT - BROADCASTED READ($rid) (QUERY REQUEST)");
    }
  }

  beb uponEvent {
    case BEB_Deliver(src, READ(readID)) => handle {
        
     /* MY CODE HERE  */
     // Got read request (query request)
     // Respond with local value v and ts (ts is a combo of timestamp and rank/pid)
     trigger(PL_Send(src, VALUE(readID, ts, wr, value)) -> pLink);
     
     println(s"$self - GOT QUERY REQUEST READ($readID) FROM $src - Responded with VALUE($readID, $ts, $wr, $value)");
     
    }
    case BEB_Deliver(src, w: WRITE) => handle {
       
     /* MY CODE HERE */
     // I am in the update phase, got an update request
     // P_j updates r = max(r, ((ts, pid), v)) and responds with ACK
     if ((w.ts, w.wr) > (ts,wr))
     {
         // This new value is newer than my value.
         // Let's update my value.
         
         ts = w.ts;
         wr = w.wr;
         value = w.writeVal;
     }
     
     trigger(PL_Send(src, ACK(w.rid)) -> pLink);
     
     println(s"$self - GOT $w FROM $src - Responded with ACK($w)");
     
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, v: VALUE) => handle {
      if (v.rid == rid) {
         
      /* MY CODE HERE  */
      // I am waiting for responses from the read request I broadcasted (query request - query phase)
      // Will save those responses, until I have a response from the majority of nodes in the system
      readlist(src) = (v.ts, v.wr, v.value);
      if (readlist.size > n/2)
      {
          // I have a read-request response from a majority of nodes (query phase responses)
          // i.e. query phase is ending. Pick max(ts, pid) from the responses I got, and find the corresponding value
          // PIDs used as tiebreakers.
          
          // Make a copy of the readlist combining the two Ints into one tuple so we can use that in maxBy
          var readlist2: Map[Address, ((Int, Int), Option[Any])] = Map.empty
          readlist foreach (x => readlist2(x._1) = ((x._2._1, x._2._2), x._2._3));
          
          var maxreadlist2: ((Int, Int), Option[Any]) = ((0, 0), None);
          maxreadlist2 = (readlist2.maxBy(_._2._1))._2; // Should return max entry in readlist by timestamp and pid
          var maxts = maxreadlist2._1._1;
          var rr = maxreadlist2._1._2;
          readval = maxreadlist2._2;

          readlist.clear;
          readlist2.clear;
          
          var bcastval: Option[Any] = None;
          
          if (reading)
          {
              // This is part of a read operation
              // Perform an update phase with the highest (ts,v) where ts is a tuple of ts and pid
              // Will broadcast the value v from the highest (ts,v) that I got from the other nodes,
              // make all others WRITE that (i.e. update their value to that)
              bcastval = readval;
          }
          else
          {
              // This is part of a write operation
              // P_i starts an update phase by sending update request 
              // with register id r and timestamp-value pair ((ts+1, i), v)
              // Make all others WRITE that (i.e. update their value to that)
              
              rr = selfRank;
              maxts = maxts + 1;
              bcastval = writeval;
          }
          
          // NOTE: What is here rr is actually wr (rank/pid)
          trigger(BEB_Broadcast(WRITE(rid, maxts, rr, bcastval)) -> beb);
          
          println(s"$self - GOT $v FROM $src - READING $reading - BROADCAST WRITE ($rid, $maxts, $rr, $bcastval)");
      }
      
     
      }
    }
    case PL_Deliver(src, v: ACK) => handle {
      if (v.rid == rid) {
  
      /* MY CODE HERE  */
      // P_i completes write (and read) when update phase ends
      acks = acks + 1;
      if (acks > n/2)
      {
          // Have got ACKs from a majority. Update phase ending.
          acks = 0;
          if (reading)
          {
              reading = false;
              trigger(AR_Read_Response(readval) -> nnar);
              println(s"$self - GOT ENOUGH ACKS - READING - RETURNED $readval");
          }
          else
          {
              trigger(AR_Write_Response() -> nnar);
              println(s"$self - GOT ENOUGH ACKS - WRITING");
          }
      }
      
      }
    }
  }
}