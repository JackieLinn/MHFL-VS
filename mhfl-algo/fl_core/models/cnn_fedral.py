import torch.nn.functional as F
from torch import nn
import torch
from fl_core.utils.tools import calc_feat_size


class OFT(nn.Module):
    """Orthogonal Fine-Tuning Module for FedRAL"""

    def __init__(self, R_dim=500):
        super(OFT, self).__init__()
        self.R = nn.Parameter(torch.randn(R_dim, R_dim))

    def forward(self, x):
        o = torch.matmul(x, self.R)
        return o


class CNN_1_OFT(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, R_dim=500, input_size=32):
        super(CNN_1_OFT, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)
        self.fc1 = nn.Linear(2 * n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 2000)
        self.fc2 = nn.Linear(2000, 500)
        self.fc3 = nn.Linear(500, out_dim)
        self.oft = OFT(R_dim=R_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        rep_oft = self.oft(rep)
        rep_total = rep + rep_oft
        out = self.fc3(rep_total)
        return out


class CNN_2_OFT(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, R_dim=500, input_size=32):
        super(CNN_2_OFT, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, n_kernels, 5)
        self.fc1 = nn.Linear(n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 2000)
        self.fc2 = nn.Linear(2000, 500)
        self.fc3 = nn.Linear(500, out_dim)
        self.oft = OFT(R_dim=R_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        rep_oft = self.oft(rep)
        rep_total = rep + rep_oft
        out = self.fc3(rep_total)
        return out


class CNN_3_OFT(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, R_dim=500, input_size=32):
        super(CNN_3_OFT, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)
        self.fc1 = nn.Linear(2 * n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 1000)
        self.fc2 = nn.Linear(1000, 500)
        self.fc3 = nn.Linear(500, out_dim)
        self.oft = OFT(R_dim=R_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        rep_oft = self.oft(rep)
        rep_total = rep + rep_oft
        out = self.fc3(rep_total)
        return out


class CNN_4_OFT(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, R_dim=500, input_size=32):
        super(CNN_4_OFT, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)
        self.fc1 = nn.Linear(2 * n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 800)
        self.fc2 = nn.Linear(800, 500)
        self.fc3 = nn.Linear(500, out_dim)
        self.oft = OFT(R_dim=R_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        rep_oft = self.oft(rep)
        rep_total = rep + rep_oft
        out = self.fc3(rep_total)
        return out


class CNN_5_OFT(nn.Module):
    def __init__(self, in_channels=3, n_kernels=16, out_dim=10, R_dim=500, input_size=32):
        super(CNN_5_OFT, self).__init__()

        self.conv1 = nn.Conv2d(in_channels, n_kernels, 5)
        self.pool = nn.MaxPool2d(2, 2)
        self.conv2 = nn.Conv2d(n_kernels, 2 * n_kernels, 5)
        self.fc1 = nn.Linear(2 * n_kernels * calc_feat_size(input_size) * calc_feat_size(input_size), 500)
        self.fc2 = nn.Linear(500, 500)
        self.fc3 = nn.Linear(500, out_dim)
        self.oft = OFT(R_dim=R_dim)

    def forward(self, x):
        x = self.pool(F.relu(self.conv1(x)))
        x = self.pool(F.relu(self.conv2(x)))
        x = x.view(x.shape[0], -1)
        x = F.relu(self.fc1(x))
        rep = F.relu(self.fc2(x))
        rep_oft = self.oft(rep)
        rep_total = rep + rep_oft
        out = self.fc3(rep_total)
        return out
