import torch.nn.functional as F
from torch import nn
from fl_core.utils.tools import calc_feat_size


class CNN_1_Proto(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_1_Proto, self).__init__()

        self.input_size = input_size

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)

        feat_size = calc_feat_size(input_size)
        self.fc_input_dim = 2 * n_kernels * feat_size * feat_size

        self.fc1 = nn.Linear(self.fc_input_dim, 2000)
        self.fc2 = nn.Linear(2000, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        out = self.fc3(rep)
        return out, rep


class CNN_2_Proto(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_2_Proto, self).__init__()

        self.input_size = input_size

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, n_kernels, 5)  # 不增加通道数

        feat_size = calc_feat_size(input_size)
        self.fc_input_dim = n_kernels * feat_size * feat_size  # 注意这里是 n_kernels

        self.fc1 = nn.Linear(self.fc_input_dim, 2000)
        self.fc2 = nn.Linear(2000, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        out = self.fc3(rep)
        return out, rep


class CNN_3_Proto(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_3_Proto, self).__init__()

        self.input_size = input_size

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)

        feat_size = calc_feat_size(input_size)
        self.fc_input_dim = 2 * n_kernels * feat_size * feat_size

        self.fc1 = nn.Linear(self.fc_input_dim, 1000)
        self.fc2 = nn.Linear(1000, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        out = self.fc3(rep)
        return out, rep


class CNN_4_Proto(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_4_Proto, self).__init__()

        self.input_size = input_size

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)

        feat_size = calc_feat_size(input_size)
        self.fc_input_dim = 2 * n_kernels * feat_size * feat_size

        self.fc1 = nn.Linear(self.fc_input_dim, 800)
        self.fc2 = nn.Linear(800, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        out = self.fc3(rep)
        return out, rep


class CNN_5_Proto(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, input_size=32):
        super(CNN_5_Proto, self).__init__()

        self.input_size = input_size

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)

        feat_size = calc_feat_size(input_size)
        self.fc_input_dim = 2 * n_kernels * feat_size * feat_size

        self.fc1 = nn.Linear(self.fc_input_dim, 500)
        self.fc2 = nn.Linear(500, 500)
        self.fc3 = nn.Linear(500, out_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        out = self.fc3(rep)
        return out, rep
