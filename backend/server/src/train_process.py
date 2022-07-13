import time
import file_utils
from multiprocessing import Process

from ml.export import export_csv
from ml.train.train import train_model
from ml.global_vars import GlobalVars


class TrainProcess(Process):
    def __init__(self, train_info_path, taskListId, taskIdList, trainId, timestamp):
        super(TrainProcess, self).__init__()
        self.train_info_path = train_info_path
        self.taskListId = taskListId
        self.taskIdList = taskIdList
        self.trainId = trainId
        self.timestamp = timestamp

    def get_trainId(self):
        return self.trainId

    def interrupt(self):
        train_info = file_utils.load_json(self.train_info_path)
        train_info['status'] = 'Interrupted'
        file_utils.save_json(train_info, self.train_info_path)

    def run(self):
        ''' The entrance of a training process. Preprocess data and start training.
            Meanwhile record the training status in training info file.
        '''
        # first export csv before training
        export_csv(self.taskListId, self.taskIdList, self.trainId, self.timestamp)

        # change status from 'Preprocessing' to 'Training'
        train_info = file_utils.load_json(self.train_info_path)
        train_info['status'] = 'Training'
        file_utils.save_json(train_info, self.train_info_path)

        # config hyperparameters
        motion_sensors = GlobalVars.MOTION_SENSORS
        config = {
            'channel_dim': len(motion_sensors) * 3,
            'sequence_dim': GlobalVars.WINDOW_LENGTH,
            'layer_dim': 1,
            'hidden_dim': 400,
            'output_dim': 2,
            'lr': 1e-3,
            'epoch': 10,
            'use_cuda': False,
        }
        
        # start training
        train_model(self.trainId, self.timestamp, config)

        # change status from 'Training' to 'Done' 
        train_info = file_utils.load_json(self.train_info_path)
        train_info['status'] = 'Done'
        file_utils.save_json(train_info, self.train_info_path)
        

if __name__ == '__main__':
    # use this code section to debug the training process
    train_info_path = '../data/train/XT12345678/XT12345678.json'
    taskListId = 'TL13r912je'
    taskIdList = ["TK51dc9xrh", "TKu0l0pg2n"]
    trainId = "XT12345678"
    timestamp = 142857142857
    
    new_process = TrainProcess(train_info_path, taskListId, taskIdList, trainId, timestamp)
    new_process.start()
    new_process.join()
