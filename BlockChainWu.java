import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
   public static final int CUT_OFF_AGE = 10;

   private ByteArrayWrapper oldest;
    private ByteArrayWrapper newest;
   private HashMap<ByteArrayWrapper, BlockNode> blocks;
    private TransactionPool txPool;
    private int maxHeight;

   // all information required in handling a block in block chain
   private class BlockNode {
      public Block b;
      public BlockNode parent;
      public ArrayList<BlockNode> children;
      public int height;
      // utxo pool for making a new block on top of this block
      private UTXOPool uPool;

      public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
         this.b = b;
         this.parent = parent;
         children = new ArrayList<BlockNode>();
         this.uPool = uPool;
         if (parent != null) {
            height = parent.height + 1;
            parent.children.add(this);
         } else {
            height = 1;
         }
      }

      public UTXOPool getUTXOPoolCopy() {
         return new UTXOPool(uPool);
      }
   }

   /* create an empty block chain with just a genesis block.
    * Assume genesis block is a valid block
    */
   public BlockChain(Block genesisBlock) {
       
       ArrayList<Transaction> transArr = new ArrayList<Transaction>();
       Transaction firstTrans = new Transaction(genesisBlock.getCoinbase());
       transArr.add(firstTrans);
       Transaction[] genTrans = transArr.toArray(new Transaction[1]);
      
      UTXOPool pool = new UTXOPool();
       ArrayList<Transaction.Output> output = firstTrans.getOutputs();
       int i = 0;
       byte[] hash = firstTrans.getHash();
      for (Transaction.Output out : output) {
        UTXO u = new UTXO(hash, i);
        pool.addUTXO(u, out);
        i++;
      }
      
      
       /*UTXOPool pool = new HashMap<UTXO, firstTrans.Output>();*/
       BlockNode node = new BlockNode(genesisBlock, null, pool);
       node.height = 0;

       this.maxHeight = node.height;

       this.oldest = new ByteArrayWrapper(genesisBlock.getHash());
       this.newest = this.oldest;

       this.blocks = new HashMap<ByteArrayWrapper, BlockNode>();
       this.blocks.put(this.oldest, node);
       this.txPool = new TransactionPool();
   }

    private BlockNode getMaxHeightBlockNode() {
        return this.blocks.get(this.newest);
    }

    private int getMaxHeight() {
        return getMaxHeightBlockNode().height;
    }

   /* Get the maximum height block
    */
   public Block getMaxHeightBlock() {
      return this.getMaxHeightBlockNode().b;
   }
   
   /* Get the UTXOPool for mining a new block on top of 
    * max height block
    */
   public UTXOPool getMaxHeightUTXOPool() {
      return this.getMaxHeightBlockNode().uPool;
   }
   
   /* Get the transaction pool to mine a new block
    */
   public TransactionPool getTransactionPool() {
      return this.txPool;
   }

   /* Add a block to block chain if it is valid.
    * For validity, all transactions should be valid
    * and block should be at height > (maxHeight - CUT_OFF_AGE).
    * For example, you can try creating a new block over genesis block 
    * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1. 
    * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
    * Return true of block is successfully added
    */
   public boolean addBlock(Block b) {
      if (b.getPrevBlockHash() == null) {
          return false;
      }
      ByteArrayWrapper prevHash = new ByteArrayWrapper(b.getPrevBlockHash());
      
      BlockNode parent = blocks.get(prevHash);
      if (parent == null) {
          return false;
      }

      UTXOPool currPool = parent.getUTXOPoolCopy();
      TxHandler handler = new TxHandler(currPool);
      ArrayList<Transaction> allTrans = b.getTransactions();
      /*allTrans.add(b.getCoinbase());*/
      int length = allTrans.size();
      Transaction[] accepted = handler.handleTxs(allTrans.toArray(new Transaction[length]));

       if (accepted.length != length) 
            return false;

       UTXOPool pool = handler.getUTXOPool();
       
       BlockNode node = new BlockNode(b, parent, pool);
       node.height = parent.height + 1;
       if (node.height <= maxHeight - CUT_OFF_AGE) {
          return false;
       }
       ByteArrayWrapper hash = new ByteArrayWrapper(b.getHash());
       this.blocks.put(hash, node);
       if (node.height > this.maxHeight) {
          this.maxHeight = node.height;
          this.newest = hash;
       }
       return true;
   }


    private boolean isValid(Block b) {
        ByteArrayWrapper prevHash = new ByteArrayWrapper(b.getPrevBlockHash());
        if (!this.blocks.containsKey(prevHash)) {
            return false;
        }

        BlockNode parent = this.blocks.get(prevHash);
        int thisHeight = parent.height + 1;

        if (thisHeight <= this.getMaxHeight() - this.CUT_OFF_AGE) {
            return false;
        }

        UTXOPool pool = parent.uPool;
        TxHandler handler = new TxHandler(pool);
        return true;
    }

   /* Add a transaction in transaction pool
    */
   public void addTransaction(Transaction tx) {
      this.txPool.addTransaction(tx);
   }
}